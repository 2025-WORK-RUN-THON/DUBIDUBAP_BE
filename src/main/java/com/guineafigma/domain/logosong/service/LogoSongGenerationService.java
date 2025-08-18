package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.event.MusicGenerationCompleteEvent;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoSongGenerationService {

    private final SunoApiService sunoApiService;
    private final LogoSongRepository logoSongRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async("musicGenerationExecutor")
    @Transactional
    public CompletableFuture<Void> generateLogoSongAsync(Long logoSongId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("비동기 로고송 생성 시작: logoSongId={}", logoSongId);
                
                // 1. LogoSong 조회
                LogoSong logoSong = logoSongRepository.findById(logoSongId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

                // 2. 가사가 없으면 에러
                if (logoSong.getLyrics() == null || logoSong.getLyrics().isEmpty()) {
                    throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
                }

                // 3. Suno API 호출
                String taskId = sunoApiService.generateMusic(logoSong);
                
                // 4. 상태 폴링 시작
                startStatusPolling(taskId);
                
                log.info("비동기 로고송 생성 요청 완료: logoSongId={}, taskId={}", logoSongId, taskId);
                
            } catch (Exception e) {
                log.error("비동기 로고송 생성 실패: logoSongId={}", logoSongId, e);
                // 실패 상태로 업데이트
                updateLogoSongStatus(logoSongId, MusicGenerationStatus.FAILED, null);
                throw new RuntimeException("로고송 생성 실패", e);
            }
        });
    }

    @Async("musicGenerationExecutor")
    @Retryable(
            value = {Exception.class},
            maxAttempts = 60, // 최대 30분 폴링 (30초 간격)
            backoff = @Backoff(delay = 30000) // 30초 대기
    )
    public void startStatusPolling(String taskId) {
        try {
            log.debug("음악 생성 상태 폴링 시작: taskId={}", taskId);
            
            // 상태 확인
            MusicGenerationResult result = sunoApiService.checkMusicStatus(taskId);
            
            if (result.getStatus() == MusicGenerationStatus.COMPLETED) {
                // 완료된 경우
                handleMusicGenerationComplete(taskId, result);
                log.info("음악 생성 완료: taskId={}", taskId);
                
            } else if (result.getStatus() == MusicGenerationStatus.FAILED) {
                // 실패한 경우
                handleMusicGenerationFailed(taskId, result);
                log.error("음악 생성 실패: taskId={}, error={}", taskId, result.getErrorMessage());
                
            } else if (result.getStatus() == MusicGenerationStatus.PROCESSING) {
                // 아직 처리 중인 경우 - 다시 폴링
                log.debug("음악 생성 진행 중: taskId={}", taskId);
                
                // 30초 후 다시 확인
                CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                    try {
                        startStatusPolling(taskId);
                    } catch (Exception e) {
                        log.error("상태 폴링 재시도 실패: taskId={}", taskId, e);
                        handleMusicGenerationFailed(taskId, MusicGenerationResult.builder()
                                .taskId(taskId)
                                .status(MusicGenerationStatus.FAILED)
                                .errorMessage("상태 폴링 실패")
                                .build());
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("음악 생성 상태 폴링 실패: taskId={}", taskId, e);
            handleMusicGenerationFailed(taskId, MusicGenerationResult.builder()
                    .taskId(taskId)
                    .status(MusicGenerationStatus.FAILED)
                    .errorMessage("상태 확인 실패: " + e.getMessage())
                    .build());
        }
    }

    @Transactional
    public void handleMusicGenerationComplete(String taskId, MusicGenerationResult result) {
        try {
            LogoSong logoSong = logoSongRepository.findBySunoTaskId(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUNO_TASK_NOT_FOUND));

            // 상태 업데이트
            logoSong.updateMusicStatus(MusicGenerationStatus.COMPLETED);
            logoSong.updateGeneratedMusicUrl(result.getAudioUrl());
            logoSongRepository.save(logoSong);

            // 완료 이벤트 발행
            eventPublisher.publishEvent(new MusicGenerationCompleteEvent(logoSong.getId(), taskId, result));
            
            log.info("음악 생성 완료 처리: logoSongId={}, taskId={}", logoSong.getId(), taskId);
            
        } catch (Exception e) {
            log.error("음악 생성 완료 처리 실패: taskId={}", taskId, e);
        }
    }

    @Transactional
    public void handleMusicGenerationFailed(String taskId, MusicGenerationResult result) {
        try {
            LogoSong logoSong = logoSongRepository.findBySunoTaskId(taskId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SUNO_TASK_NOT_FOUND));

            // 실패 상태로 업데이트
            logoSong.updateMusicStatus(MusicGenerationStatus.FAILED);
            logoSongRepository.save(logoSong);
            
            log.error("음악 생성 실패 처리: logoSongId={}, taskId={}, error={}", 
                    logoSong.getId(), taskId, result.getErrorMessage());
                    
        } catch (Exception e) {
            log.error("음악 생성 실패 처리 중 에러: taskId={}", taskId, e);
        }
    }

    private void updateLogoSongStatus(Long logoSongId, MusicGenerationStatus status, String audioUrl) {
        try {
            LogoSong logoSong = logoSongRepository.findById(logoSongId).orElse(null);
            if (logoSong != null) {
                logoSong.updateMusicStatus(status);
                if (audioUrl != null) {
                    logoSong.updateGeneratedMusicUrl(audioUrl);
                }
                logoSongRepository.save(logoSong);
            }
        } catch (Exception e) {
            log.error("로고송 상태 업데이트 실패: logoSongId={}", logoSongId, e);
        }
    }

    /**
     * 진행 중인 모든 음악 생성 작업의 상태를 확인하는 배치 작업
     */
    @Async("musicGenerationExecutor")
    public void checkAllProcessingTasks() {
        try {
            List<LogoSong> processingLogoSongs = logoSongRepository.findByMusicStatus(MusicGenerationStatus.PROCESSING);
            
            log.info("진행 중인 음악 생성 작업 확인: count={}", processingLogoSongs.size());
            
            for (LogoSong logoSong : processingLogoSongs) {
                if (logoSong.getSunoTaskId() != null) {
                    try {
                        MusicGenerationResult result = sunoApiService.checkMusicStatus(logoSong.getSunoTaskId());
                        
                        if (result.getStatus() == MusicGenerationStatus.COMPLETED) {
                            handleMusicGenerationComplete(logoSong.getSunoTaskId(), result);
                        } else if (result.getStatus() == MusicGenerationStatus.FAILED) {
                            handleMusicGenerationFailed(logoSong.getSunoTaskId(), result);
                        }
                        // PROCESSING 상태면 계속 대기
                        
                    } catch (Exception e) {
                        log.error("배치 상태 확인 실패: logoSongId={}, taskId={}", 
                                logoSong.getId(), logoSong.getSunoTaskId(), e);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("진행 중인 작업 상태 확인 배치 실패", e);
        }
    }
}