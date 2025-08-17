package com.guineafigma.domain.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaPathService {

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    @Value("${cloud.aws.region.static}")
    private String region;

    private static Pattern tempUrlPattern = null;

    // temp 경로 생성
    public String generateTempPath(Long userId) {
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

    private String getS3ImageUrlPattern(String pathPattern) {
        return String.format(
                "https://%s\\.s3\\.%s\\.amazonaws\\.com/%s\\.(jpg|jpeg|png|gif|webp)",
                java.util.regex.Pattern.quote(bucketName),
                java.util.regex.Pattern.quote(region),
                pathPattern);
    }

    public String getTempImageUrlPattern() {
        return getS3ImageUrlPattern("users/\\d+/temp/.*?");
    }

    public String getAllImageUrlPattern() {
        return getS3ImageUrlPattern("[^\\\\s\"'<>]+");
    }

    public List<String> extractTempImageUrls(String content) {
        if (tempUrlPattern == null) {
            String pattern = getTempImageUrlPattern();
            log.info("temp URL 정규식 패턴: {}", pattern);
            tempUrlPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        
        Matcher matcher = tempUrlPattern.matcher(content);
        List<String> tempUrls = new ArrayList<>();
        while (matcher.find()) {
            String foundUrl = matcher.group();
            tempUrls.add(foundUrl);
        }
        return tempUrls;
    }

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
        return imageUrls;
    }

    public boolean isTempImage(String s3Key) {
        return s3Key != null && s3Key.contains("/temp/");
    }
}


