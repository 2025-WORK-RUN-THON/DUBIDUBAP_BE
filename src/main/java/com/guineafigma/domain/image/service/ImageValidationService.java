package com.guineafigma.domain.image.service;

import com.guineafigma.domain.image.dto.request.ImageUploadRequest;
import com.guineafigma.domain.image.enums.ImageType;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageValidationService {

    private static final int MAX_FILES_PER_UPLOAD = 10;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB

    public void validateUploadRequest(ImageUploadRequest request) {
        validateFile(request.getFile());
        validateUploadPath(request.getUploadPath());
    }

    public void validateMultipleFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }
        
        if (files.size() > MAX_FILES_PER_UPLOAD) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        files.forEach(this::validateFile);
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }
        
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_TOO_LARGE);
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.IMAGE_FORMAT_NOT_SUPPORTED);
        }
    }

    public void validateUserImageType(ImageType imageType) {
        if (!imageType.name().startsWith("USER_")) {
            throw new IllegalArgumentException("USER 타입 이미지만 처리 가능합니다: " + imageType);
        }
    }

    public void validateSystemImageType(ImageType imageType) {
        if (!imageType.name().startsWith("SYSTEM_")) {
            throw new IllegalArgumentException("SYSTEM 타입 이미지만 처리 가능합니다: " + imageType);
        }
    }

    public void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

    private void validateUploadPath(String uploadPath) {
        if (uploadPath == null || uploadPath.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // 경로 보안 검증 (../ 등 상위 디렉토리 접근 차단)
        if (uploadPath.contains("..") || uploadPath.startsWith("/")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}