package com.guineafigma.domain.media.service;

import com.guineafigma.domain.media.entity.Media;
import com.guineafigma.domain.media.repository.MediaRepository;
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
public class MediaCleanupService {

    private final MediaRepository mediaRepository;
    private final MediaStorageService mediaStorageService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void cleanupOrphanedMedia() {
        log.info("고아 미디어 삭제 작업을 시작합니다.");
        try {
            cleanupDbOrphanedTempMedia();
            cleanupS3OrphanedFiles();
            log.info("고아 미디어 삭제 작업이 완료되었습니다.");
        } catch (Exception e) {
            log.error("고아 미디어 삭제 작업 중 오류", e);
        }
    }

    private void cleanupDbOrphanedTempMedia() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<Media> orphaned = mediaRepository.findAllByCreatedAtBeforeAndS3KeyContaining(cutoffTime, "/temp/");
        for (Media media : orphaned) {
            try {
                mediaStorageService.deleteFromS3(media.getS3Key());
                mediaRepository.delete(media);
            } catch (Exception e) {
                log.error("DB temp 미디어 삭제 실패: id={}, key={}", media.getId(), media.getS3Key(), e);
            }
        }
    }

    private void cleanupS3OrphanedFiles() {
        String tempPrefix = "users/";
        List<String> s3TempFiles = mediaStorageService.listTempFiles(tempPrefix)
                .stream()
                .filter(key -> key.contains("/temp/"))
                .toList();

        if (s3TempFiles.isEmpty()) return;

        Set<String> dbS3Keys = mediaRepository.findAll().stream()
                .map(Media::getS3Key)
                .collect(Collectors.toSet());

        List<String> orphanedS3Files = s3TempFiles.stream()
                .filter(s3Key -> !dbS3Keys.contains(s3Key))
                .toList();

        for (String s3Key : orphanedS3Files) {
            try {
                mediaStorageService.deleteFromS3(s3Key);
            } catch (Exception e) {
                log.error("S3 고아 파일 삭제 실패: {}", s3Key, e);
            }
        }
    }
}


