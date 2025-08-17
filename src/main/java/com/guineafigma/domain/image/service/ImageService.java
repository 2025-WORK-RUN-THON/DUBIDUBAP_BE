package com.guineafigma.domain.image.service;

import com.guineafigma.domain.image.dto.request.ImageUploadRequest;
import com.guineafigma.domain.image.dto.response.ImageResponse;
import com.guineafigma.domain.image.dto.response.MultipleImageUploadResponse;
import com.guineafigma.domain.image.entity.Image;
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
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {
    
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