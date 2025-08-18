package com.guineafigma.domain.media.service;

import com.guineafigma.domain.media.dto.request.MediaUploadRequest;
import com.guineafigma.domain.media.dto.response.MediaResponse;
import com.guineafigma.domain.media.dto.response.MultipleMediaUploadResponse;
import com.guineafigma.domain.media.entity.Media;
import com.guineafigma.domain.media.repository.MediaRepository;
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
public class MediaService {

    private final MediaRepository mediaRepository;
    private final MediaStorageService mediaStorageService;
    private final MediaPathService mediaPathService;
    private final MediaValidationService mediaValidationService;

    @Transactional
    public MultipleMediaUploadResponse uploadTempMedia(List<MultipartFile> files, Long userId) {
        mediaValidationService.validateMultipleFiles(files);
        
        List<MediaResponse> uploaded = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                MediaResponse response = uploadSingleTempMedia(file, userId);
                uploaded.add(response);
            } catch (Exception e) {
                log.error("임시 업로드 실패: {}, 사용자: {}", file.getOriginalFilename(), userId, e);
            }
        }
        
        log.info("임시 미디어 업로드 완료 - 성공: {}/{}, 사용자: {}", uploaded.size(), files.size(), userId);
        return MultipleMediaUploadResponse.of(uploaded);
    }

    private MediaResponse uploadSingleTempMedia(MultipartFile file, Long userId) {
        try {
            ImageMetadata metadata = extractImageMetadata(file);
            String tempPath = mediaPathService.generateTempPath(userId);
            String fileName = mediaStorageService.generateUniqueFileName(file.getOriginalFilename());
            String tempS3Key = tempPath + "/" + fileName;
            
            mediaStorageService.uploadToS3(tempS3Key, file);
            
            Media temp = Media.builder()
                    .s3Key(tempS3Key)
                    .originalFilename(file.getOriginalFilename())
                    .width(metadata.width)
                    .height(metadata.height)
                    .userId(userId)
                    .logosongId(null)
                    .build();
            
            Media saved = mediaRepository.save(temp);
            String tempUrl = mediaStorageService.generatePublicUrl(tempS3Key);
            return MediaResponse.from(saved, tempUrl);
        } catch (IOException e) {
            log.error("미디어 처리 중 오류 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MEDIA_UPLOAD_FAILED);
        }
    }

    @Transactional
    public MediaResponse uploadMedia(MediaUploadRequest request) {
        mediaValidationService.validateUploadRequest(request);
        try {
            ImageMetadata metadata = extractImageMetadata(request.getFile());
            String fileName = mediaStorageService.generateUniqueFileName(request.getFile().getOriginalFilename());
            String s3Key = mediaStorageService.generateS3Key(request.getUploadPath(), fileName);
            mediaStorageService.uploadToS3(s3Key, request.getFile());
            
            Media media = Media.builder()
                    .s3Key(s3Key)
                    .originalFilename(request.getFile().getOriginalFilename())
                    .width(metadata.width)
                    .height(metadata.height)
                    .userId(mediaPathService.extractUserIdFromPath(request.getUploadPath()))
                    .logosongId(null)
                    .build();
            Media saved = mediaRepository.save(media);
            return MediaResponse.from(saved, mediaStorageService.generatePublicUrl(s3Key));
        } catch (IOException e) {
            log.error("미디어 업로드 중 오류 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.MEDIA_UPLOAD_FAILED);
        }
    }

    public List<MediaResponse> getMediaByPath(String pathPrefix) {
        return mediaRepository.findByS3KeyStartingWith(pathPrefix).stream()
                .map(m -> MediaResponse.from(m, mediaStorageService.generatePublicUrl(m.getS3Key())))
                .collect(Collectors.toList());
    }

    public MediaResponse getMediaById(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        return MediaResponse.from(media, mediaStorageService.generatePublicUrl(media.getS3Key()));
    }

    @Transactional
    public void deleteMedia(Long mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        mediaStorageService.deleteFromS3(media.getS3Key());
        mediaRepository.delete(media);
    }

    private ImageMetadata extractImageMetadata(MultipartFile file) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(file.getInputStream());
        Long width = bufferedImage != null ? (long) bufferedImage.getWidth() : null;
        Long height = bufferedImage != null ? (long) bufferedImage.getHeight() : null;
        return new ImageMetadata(width, height);
    }

    private static class ImageMetadata {
        final Long width;
        final Long height;
        ImageMetadata(Long width, Long height) {
            this.width = width;
            this.height = height;
        }
    }
}


