package com.guineafigma.domain.media.service;

import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaStorageService {

    private final S3Client s3Client;
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    @Value("${cloud.aws.region.static}")
    private String region;

    public void uploadToS3(String s3Key, MultipartFile file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, 
                            RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("S3 업로드 완료: {}", s3Key);
        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteFromS3(String s3Key) {
        try {
            s3Client.deleteObject(builder -> builder.bucket(bucketName).key(s3Key));
            log.info("S3 삭제 완료: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 삭제 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void moveFile(String sourceKey, String destinationKey) {
        try {
            s3Client.copyObject(builder -> builder
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
            );
            log.info("S3 파일 복사 완료: {} -> {}", sourceKey, destinationKey);
            
            if (verifyFileExists(destinationKey)) {
                deleteFromS3(sourceKey);
                log.info("S3 파일 이동 완료: {} -> {}", sourceKey, destinationKey);
            } else {
                log.error("파일 복사 검증 실패: {}", destinationKey);
                throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
            
        } catch (Exception e) {
            log.error("S3 파일 이동 실패: {} -> {}", sourceKey, destinationKey, e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private boolean verifyFileExists(String s3Key) {
        try {
            s3Client.headObject(builder -> builder.bucket(bucketName).key(s3Key));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void deleteFiles(List<String> s3Keys) {
        for (String s3Key : s3Keys) {
            try {
                deleteFromS3(s3Key);
            } catch (Exception e) {
                log.error("배치 삭제 중 실패한 파일: {}", s3Key, e);
            }
        }
    }

    public String generatePublicUrl(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }

    public String extractS3KeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_URL_INVALID_FORMAT);
        }
        
        try {
            String[] parts = imageUrl.split(".amazonaws.com/", 2);
            if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                return parts[1];
            }
            log.error("S3 URL 형식 오류: {}", imageUrl);
            throw new BusinessException(ErrorCode.IMAGE_URL_INVALID_FORMAT);
        } catch (BusinessException e) {
            throw e; 
        } catch (Exception e) {
            log.error("S3 키 추출 중 예외 발생: url={}", imageUrl, e);
            throw new BusinessException(ErrorCode.IMAGE_URL_INVALID_FORMAT);
        }
    }

    public String generateUniqueFileName(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        return uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    public String generateS3Key(String uploadPath, String fileName) {
        String normalizedPath = uploadPath.endsWith("/") ? uploadPath : uploadPath + "/";
        return normalizedPath + fileName;
    }

    public List<String> listTempFiles(String tempPrefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(tempPrefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            
            return response.contents().stream()
                    .map(S3Object::key)
                    .toList();
                    
        } catch (Exception e) {
            log.error("S3 temp 파일 목록 조회 실패: prefix={}", tempPrefix, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}


