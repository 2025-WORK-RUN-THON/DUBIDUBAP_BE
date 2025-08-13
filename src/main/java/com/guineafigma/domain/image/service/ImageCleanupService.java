package com.guineafigma.domain.image.service;

import com.guineafigma.domain.image.entity.Image;
import com.guineafigma.domain.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupService {

    private final ImageRepository imageRepository;
    private final ImageStorageService imageStorageService;

    /**
     * 매일 한국시간 새벽 3시에 고아 이미지들을 삭제
     * 1. DB의 고아 이미지: postId가 null이고 s3Key에 '/temp/' 경로가 포함되며 생성된 지 24시간이 지난 이미지
     * 2. S3의 고아 파일: S3에는 있지만 DB에는 없는 temp 경로의 파일들
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOrphanedImages() {
        log.info("고아 이미지 삭제 작업을 시작합니다.");
        
        try {
            // 1. DB의 temp 경로 고아 이미지들 삭제
            cleanupDbOrphanedTempImages();
            
            // 2. S3의 고아 파일들 삭제
            cleanupS3OrphanedFiles();
            
            log.info("고아 이미지 삭제 작업이 완료되었습니다.");
            
        } catch (Exception e) {
            log.error("고아 이미지 삭제 작업 중 오류가 발생했습니다.", e);
        }
    }
    
    private void cleanupDbOrphanedTempImages() {
        log.info("DB의 temp 경로 고아 이미지 삭제를 시작합니다.");
        
        // 24시간 전 시간 계산
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        log.info("삭제 기준 시간: {}", cutoffTime);
        
        // 삭제 대상 temp 경로 고아 이미지들 조회
        List<Image> orphanedTempImages = imageRepository.findOrphanedTempImages(cutoffTime);
        
        if (orphanedTempImages.isEmpty()) {
            log.info("삭제할 DB의 temp 경로 고아 이미지가 없습니다.");
            return;
        }
        
        log.info("{}개의 DB temp 경로 고아 이미지를 삭제합니다.", orphanedTempImages.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        // 각 이미지에 대해 S3와 DB에서 삭제
        for (Image image : orphanedTempImages) {
            try {
                // S3에서 이미지 파일 삭제
                imageStorageService.deleteFromS3(image.getS3Key());
                
                // DB에서 이미지 정보 삭제
                imageRepository.delete(image);
                
                successCount++;
                log.debug("DB temp 이미지 삭제 완료: ID={}, S3Key={}", image.getId(), image.getS3Key());
                
            } catch (Exception e) {
                failureCount++;
                log.error("DB temp 이미지 삭제 실패: ID={}, S3Key={}, 오류={}", 
                        image.getId(), image.getS3Key(), e.getMessage(), e);
            }
        }
        
        log.info("DB temp 경로 고아 이미지 삭제가 완료되었습니다. 성공: {}개, 실패: {}개", successCount, failureCount);
    }
    
    private void cleanupS3OrphanedFiles() {
        log.info("S3 고아 파일 삭제를 시작합니다.");
        
        // S3에서 temp 경로의 모든 파일 목록 조회
        String tempPrefix = "users/";
        List<String> s3TempFiles = imageStorageService.listTempFiles(tempPrefix)
                .stream()
                .filter(key -> key.contains("/temp/"))
                .toList();
        
        if (s3TempFiles.isEmpty()) {
            log.info("S3에 temp 파일이 없습니다.");
            return;
        }
        
        log.info("S3에서 {}개의 temp 파일을 발견했습니다.", s3TempFiles.size());
        
        // DB에 존재하는 S3 키 목록 조회
        Set<String> dbS3Keys = imageRepository.findAll()
                .stream()
                .map(Image::getS3Key)
                .collect(Collectors.toSet());
        
        // S3에는 있지만 DB에는 없는 고아 파일들 찾기
        List<String> orphanedS3Files = s3TempFiles.stream()
                .filter(s3Key -> !dbS3Keys.contains(s3Key))
                .toList();
        
        if (orphanedS3Files.isEmpty()) {
            log.info("삭제할 S3 고아 파일이 없습니다.");
            return;
        }
        
        log.info("{}개의 S3 고아 파일을 삭제합니다.", orphanedS3Files.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        // S3에서 고아 파일들 삭제
        for (String s3Key : orphanedS3Files) {
            try {
                imageStorageService.deleteFromS3(s3Key);
                successCount++;
                log.debug("S3 고아 파일 삭제 완료: {}", s3Key);
                
            } catch (Exception e) {
                failureCount++;
                log.error("S3 고아 파일 삭제 실패: S3Key={}, 오류={}", s3Key, e.getMessage(), e);
            }
        }
        
        log.info("S3 고아 파일 삭제가 완료되었습니다. 성공: {}개, 실패: {}개", successCount, failureCount);
    }
}