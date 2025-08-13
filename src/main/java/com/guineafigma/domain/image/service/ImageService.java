package com.guineafigma.domain.image.service;

import com.guineafigma.domain.image.dto.request.ImageUploadRequest;
import com.guineafigma.domain.image.dto.response.ImageResponse;
import com.guineafigma.domain.image.dto.response.MultipleImageUploadResponse;
import com.guineafigma.domain.image.entity.Image;
import com.guineafigma.domain.image.enums.ImageType;
import com.guineafigma.domain.image.repository.ImageRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    // 상수 정의
    private static final int TEMP_IMAGE_EXPIRY_HOURS = 24;

    private final ImageRepository imageRepository;
    private final ImageStorageService imageStorageService;
    private final ImagePathService imagePathService;
    private final ImageValidationService imageValidationService;

    // 다중 임시 이미지 업로드
    @Transactional
    public MultipleImageUploadResponse uploadTempImages(List<MultipartFile> files, Long userId) {
        imageValidationService.validateMultipleFiles(files);
        
        List<ImageResponse> uploadedImages = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                ImageResponse imageResponse = uploadSingleTempImage(file, userId);
                uploadedImages.add(imageResponse);
            } catch (Exception e) {
                log.error("임시 이미지 업로드 실패: {}, 사용자: {}", file.getOriginalFilename(), userId, e);
            }
        }
        
        log.info("임시 이미지 업로드 완료 - 성공: {}/{}, 사용자: {}", 
                uploadedImages.size(), files.size(), userId);
        
        return MultipleImageUploadResponse.of(uploadedImages);
    }

    // 단일 임시 이미지 업로드
    private ImageResponse uploadSingleTempImage(MultipartFile file, Long userId) {
        try {
            ImageMetadata metadata = extractImageMetadata(file);
            
            // temp 경로 생성
            String tempPath = imagePathService.generateTempPath(userId);
            String fileName = imageStorageService.generateUniqueFileName(file.getOriginalFilename());
            String tempS3Key = tempPath + "/" + fileName;
            
            // S3 업로드
            imageStorageService.uploadToS3(tempS3Key, file);
            
            // DB 저장 (temp 이미지는 postId null)
            Image tempImage = Image.builder()
                    .s3Key(tempS3Key)
                    .type(null)
                    .originalFilename(file.getOriginalFilename())
                    .width(metadata.width)
                    .height(metadata.height)
                    .userId(userId)
                    .postId(null)
                    .build();
            
            Image savedImage = imageRepository.save(tempImage);
            String tempUrl = imageStorageService.generatePublicUrl(tempS3Key);
            
            return ImageResponse.from(savedImage, tempUrl);
            
        } catch (IOException e) {
            log.error("이미지 처리 중 오류 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 본문에서 temp 이미지 URL을 찾아서 permanent URL로 변환 
    @Transactional
    public String processContentAndMigrateImages(String content, ImageType imageType, Long userId, Long postId) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        List<String> tempUrls = imagePathService.extractTempImageUrls(content);
        if (tempUrls.isEmpty()) {
            return content;
        }

        Map<String, String> urlMappings = new HashMap<>();
        for (String tempUrl : tempUrls) {
            try {
                String permanentUrl = migrateTempImageByUrl(tempUrl, imageType, userId, postId);
                urlMappings.put(tempUrl, permanentUrl);
            } catch (Exception e) {
                log.error("이미지 이동 실패: tempUrl={}, userId={}", tempUrl, userId, e);
            }
        }

        return imagePathService.replaceTempUrls(content, urlMappings);
    }
    
    // 게시글 수정 시 삭제된 이미지 처리 
    @Transactional
    public void processDeletedImagesAfterPostUpdate(ImageType imageType, Long postId, String newContent) {
        // 현재 게시글에 연결된 이미지 목록 조회
        List<Image> currentImages = imageRepository.findByTypeAndPostId(imageType, postId);

        if (currentImages.isEmpty()) {
            return;
        }

        // 새 컨텐츠에서 이미지 URL 추출
        List<String> newImageUrls = imagePathService.extractAllImageUrls(newContent);

        // 삭제할 이미지 찾기 (현재 DB에는 있지만 새 컨텐츠에는 없는 이미지)
        List<Image> imagesToDelete = currentImages.stream()
                .filter(image -> {
                    String imageUrl = imageStorageService.generatePublicUrl(image.getS3Key());
                    return !newImageUrls.contains(imageUrl);
                })
                .collect(Collectors.toList());

        // 삭제 처리
        for (Image imageToDelete : imagesToDelete) {
            try {
                deleteImage(imageToDelete.getId());
                log.info("게시글 수정으로 인한 이미지 삭제: imageId={}, postId={}, imageType={}",
                        imageToDelete.getId(), postId, imageType);
            } catch (Exception e) {
                log.error("이미지 삭제 실패: imageId={}, postId={}", imageToDelete.getId(), postId, e);
            }
        }
    }

    // 통합 이미지 처리 메서드 - 게시글 생성/수정 시 사용 (다른 서비스에서 사용해야하는 메서드)
    @Transactional
    public String processContentAndUpdateImages(String content, ImageType imageType, Long userId, Long postId, String previousContent) {
        if (content == null || content.trim().isEmpty()) {
            // 컨텐츠가 비어있지만 이전 컨텐츠가 있다면 기존 이미지들 삭제
            if (previousContent != null && !previousContent.trim().isEmpty()) {
                processDeletedImagesAfterPostUpdate(imageType, postId, "");
            }
            return content;
        }

        // 새로 추가된 임시 이미지를 영구 경로로 마이그레이션
        String updatedContent = processContentAndMigrateImages(content, imageType, userId, postId);
        
        // 기존 이미지 중 더 이상 사용되지 않는 것들 삭제 (수정 시에만)
        if (previousContent != null) {
            processDeletedImagesAfterPostUpdate(imageType, postId, updatedContent);
        }
        
        log.info("통합 이미지 처리 완료: postId={}, imageType={}, userId={}", postId, imageType, userId);
        
        return updatedContent;
    }
    
    // 이미지 업로드 메인 로직
    @Transactional
    public ImageResponse uploadImage(ImageUploadRequest request) {
        imageValidationService.validateUploadRequest(request);
        
        try {
            ImageMetadata metadata = extractImageMetadata(request.getFile());
            
            log.debug("이미지 크기 - width: {}, height: {}", metadata.width, metadata.height);
            
            // 파일명 생성
            String fileName = imageStorageService.generateUniqueFileName(request.getFile().getOriginalFilename());
            
            // S3 키 생성
            String s3Key = imageStorageService.generateS3Key(request.getUploadPath(), fileName);
            
            // S3에 업로드
            imageStorageService.uploadToS3(s3Key, request.getFile());
            
            // DB에 저장
            Image image = Image.builder()
                    .s3Key(s3Key)
                    .type(null)
                    .originalFilename(request.getFile().getOriginalFilename())
                    .width(metadata.width)
                    .height(metadata.height)
                    .userId(imagePathService.extractUserIdFromPath(request.getUploadPath()))
                    .postId(null)
                    .build();
            
            Image savedImage = imageRepository.save(image);
            
            String fileUrl = imageStorageService.generatePublicUrl(s3Key);
            return ImageResponse.from(savedImage, fileUrl);
            
        } catch (IOException e) {
            log.error("이미지 업로드 중 오류 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 경로 패턴으로 이미지 목록 조회
    public List<ImageResponse> getImagesByPath(String pathPattern) {
        List<Image> images = imageRepository.findByS3KeyStartingWith(pathPattern);
        return images.stream()
                .map(image -> ImageResponse.from(image, imageStorageService.generatePublicUrl(image.getS3Key())))
                .collect(Collectors.toList());
    }

    // 이미지 상세 조회 (URL 포함)
    public ImageResponse getImageById(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        String fileUrl = imageStorageService.generatePublicUrl(image.getS3Key());
        return ImageResponse.from(image, fileUrl);
    }
    
    // 이미지 삭제
    @Transactional
    public void deleteImage(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // S3에서 삭제
        imageStorageService.deleteFromS3(image.getS3Key());
        
        // DB에서 삭제
        imageRepository.delete(image);
    }


    // 타입별 이미지 업로드
    @Transactional
    public ImageResponse uploadImageByType(ImageType imageType, MultipartFile file, Long userId, Long postId) {
        // 업로드 경로 생성
        String uploadPath;
        if (imageType.name().startsWith("USER_")) {
            uploadPath = imagePathService.generateUserUploadPath(imageType, userId, postId.toString());
        } else {
            uploadPath = imagePathService.generateSystemUploadPath(imageType, postId.toString());
        }
        
        ImageUploadRequest request = ImageUploadRequest.builder()
                .file(file)
                .uploadPath(uploadPath)
                .build();
        
        ImageResponse response = uploadImage(request);
        
        Image savedImage = imageRepository.findById(response.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        savedImage.updateType(imageType);

        // 업데이트된 이미지로 응답 생성
        return ImageResponse.from(savedImage, response.getFileUrl());
    }

    @Transactional
    public ImageResponse uploadSystemImage(ImageType imageType, MultipartFile file, Long postId) {
        String uploadPath = imagePathService.generateSystemUploadPath(imageType, postId.toString());

        ImageUploadRequest request = ImageUploadRequest.builder()
                .file(file)
                .uploadPath(uploadPath)
                .build();

        ImageResponse response = uploadImage(request);

        Image savedImage = imageRepository.findById(response.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        savedImage.updateType(imageType);
        savedImage.updatePostId(postId);
        return ImageResponse.from(savedImage, response.getFileUrl());
    }

    public List<ImageResponse> getImagesByType(ImageType imageType, Long userId, Long postId) {
        // 타입에 따라 적절한 경로 생성
        String uploadPath;
        if (imageType.name().startsWith("USER_")) {
            uploadPath = imagePathService.generateUserUploadPath(imageType, userId, postId.toString());
        } else {
            uploadPath = imagePathService.generateSystemUploadPath(imageType, postId.toString());
        }
        
        List<Image> images = imageRepository.findByS3KeyStartingWith(uploadPath);
        return images.stream()
                .map(image -> ImageResponse.from(image, imageStorageService.generatePublicUrl(image.getS3Key())))
                .collect(Collectors.toList());
    }


    // temp URL을 permanent URL로 이동 처리
    private String migrateTempImageByUrl(String tempUrl, ImageType imageType, Long userId, Long postId) {
        String tempS3Key = imageStorageService.extractS3KeyFromUrl(tempUrl);
        
        Image tempImage = imageRepository.findByS3Key(tempS3Key)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        
        if (!userId.equals(tempImage.getUserId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        if (isExpiredTempImage(tempImage)){
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        // permanent 경로 생성
        String permanentBasePath = imagePathService.generateUserUploadPath(imageType, userId, postId.toString());
        String newFileName = imageStorageService.generateUniqueFileName(tempImage.getOriginalFilename());
        String newS3Key = imageStorageService.generateS3Key(permanentBasePath, newFileName);
        
        imageStorageService.moveFile(tempS3Key, newS3Key);
        
        tempImage.updateS3Key(newS3Key);
        tempImage.updateType(imageType);
        tempImage.updatePostId(postId); // postId 설정
        
        log.info("temp → permanent 이동 완료: imageId={}, postId={}", tempImage.getId(), postId);
        
        return imageStorageService.generatePublicUrl(newS3Key);
    }




    // 만료된 temp 이미지인지 확인
    private boolean isExpiredTempImage(Image image) {
        return imagePathService.isTempImage(image.getS3Key()) && 
            image.getCreatedAt().isBefore(LocalDateTime.now().minusHours(TEMP_IMAGE_EXPIRY_HOURS));
    }
    
    // postId로 이미지 목록 조회
    public List<Image> findByTypeAndPostId(ImageType imageType, Long postId) {
        return imageRepository.findByTypeAndPostId(imageType, postId);
    }
    
    
    // 이미지 메타데이터 추출
    private ImageMetadata extractImageMetadata(MultipartFile file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        Long width = bufferedImage != null ? (long) bufferedImage.getWidth() : null;
        Long height = bufferedImage != null ? (long) bufferedImage.getHeight() : null;
        return new ImageMetadata(width, height);
    }
    
    // 이미지 메타데이터 내부 클래스
    private static class ImageMetadata {
        final Long width;
        final Long height;
        
        ImageMetadata(Long width, Long height) {
            this.width = width;
            this.height = height;
        }
    }

} 