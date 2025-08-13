package com.guineafigma.domain.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.guineafigma.domain.image.enums.ImageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImagePathService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    @Value("${cloud.aws.region.static}")
    private String region;

    private static Pattern tempUrlPattern = null;
    
    private final ImageValidationService imageValidationService;

    // 유저 이미지 업로드 경로 생성
    public String generateUserUploadPath(ImageType imageType, Long userId, String additionalPath) {
        imageValidationService.validateUserImageType(imageType);
        imageValidationService.validateUserId(userId);
        
        String typeWithoutPrefix = extractTypeWithoutPrefix(imageType.name(), "USER_");
        
        StringBuilder path = new StringBuilder("users/")
                .append(userId)
                .append("/")
                .append(typeWithoutPrefix);
        
        if (additionalPath != null && !additionalPath.trim().isEmpty()) {
            appendPath(path, additionalPath);
        }
        
        return path.toString();
    }
    
    // 시스템 이미지 업로드 경로 생성
    public String generateSystemUploadPath(ImageType imageType, String additionalPath) {
        imageValidationService.validateSystemImageType(imageType);
        
        String typeWithoutPrefix = extractTypeWithoutPrefix(imageType.name(), "SYSTEM_");
        
        StringBuilder path = new StringBuilder("system/")
                .append(typeWithoutPrefix); // system/dogam
        
        if (additionalPath != null && !additionalPath.trim().isEmpty()) {
            appendPath(path, additionalPath);
        }
        
        return path.toString();
    }

    // temp 경로 생성
    public String generateTempPath(Long userId) {
        imageValidationService.validateUserId(userId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("users/%d/temp/%s_%s", userId, timestamp, randomId);
    }

    // 경로에서 사용자 ID 추출
    public Long extractUserIdFromPath(String uploadPath) {
        if (uploadPath != null && uploadPath.startsWith("users/")) {
            String[] parts = uploadPath.split("/");
            if (parts.length >= 2) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    // S3 이미지 URL 패턴 생성 
    private String getS3ImageUrlPattern(String pathPattern) {
        return String.format(
                "https://%s\\.s3\\.%s\\.amazonaws\\.com/%s\\.(jpg|jpeg|png|gif|webp)",
                java.util.regex.Pattern.quote(bucketName),
                java.util.regex.Pattern.quote(region),
                pathPattern);
    }
    
    // temp 이미지 URL 패턴 생성
    public String getTempImageUrlPattern() {
        return getS3ImageUrlPattern("users/\\d+/temp/.*?");
    }

    // 모든 이미지 URL 패턴 생성 (temp + permanent)
    public String getAllImageUrlPattern() {
        return getS3ImageUrlPattern("[^\\\\s\"'<>]+");
    }

    // 본문에서 temp 이미지 URL 패턴 추출
    public List<String> extractTempImageUrls(String content) {
        if (tempUrlPattern == null) {
            String pattern = getTempImageUrlPattern();
            log.info("temp URL 정규식 패턴: {}", pattern);
            tempUrlPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        
        log.info("컨텐츠에서 temp URL 추출 시도: {}", content);
        
        Matcher matcher = tempUrlPattern.matcher(content);
        List<String> tempUrls = new ArrayList<>();
        while (matcher.find()) {
            String foundUrl = matcher.group();
            tempUrls.add(foundUrl);
            log.info("temp URL 발견: {}", foundUrl);
        }
        
        log.info("총 발견된 temp URL 개수: {}", tempUrls.size());
        return tempUrls;
    }

    // 컨텐츠에서 모든 이미지 URL 추출 (temp 이미지뿐만 아니라 permanent 이미지도)
    public List<String> extractAllImageUrls(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String allImagePattern = getAllImageUrlPattern();
        Pattern pattern = Pattern.compile(allImagePattern, Pattern.CASE_INSENSITIVE);
        
        Matcher matcher = pattern.matcher(content);
        List<String> imageUrls = new ArrayList<>();
        while (matcher.find()) {
            imageUrls.add(matcher.group());
        }
        
        log.debug("컨텐츠에서 추출된 이미지 URL 개수: {}", imageUrls.size());
        return imageUrls;
    }

    // 본문의 temp URL을 permanent URL로 교체
    public String replaceTempUrls(String content, Map<String, String> urlMappings) {
        String result = content;
        for (Map.Entry<String, String> entry : urlMappings.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    // temp 경로 이미지인지 확인
    public boolean isTempImage(String s3Key) {
        return s3Key != null && s3Key.contains("/temp/");
    }

    private String extractTypeWithoutPrefix(String typeName, String prefix) {
        return typeName.substring(prefix.length()).toLowerCase();
    }

    private void appendPath(StringBuilder path, String additionalPath) {
        if (!additionalPath.startsWith("/")) {
            path.append("/");
        }
        path.append(additionalPath);
    }
} 