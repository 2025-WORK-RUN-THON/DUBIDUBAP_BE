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
    public void generateLogoSongAsync(Long logoSongId) {
        try {
            log.info("비동기 로고송 생성 시작: logoSongId={}", logoSongId);

            // 1. LogoSong 조회 (커밋 이후 안전하게 조회됨)
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
            // 실패 상태로 업데이트 (트랜잭션 경계 내부에서 처리)
            updateLogoSongStatus(logoSongId, MusicGenerationStatus.FAILED, null);
            // 비동기에서 상위로 예외 전파하지 않음 (UnexpectedRollback 방지)
        }
    }

    @Async("musicGenerationExecutor")
    public void startStatusPolling(String taskId) {
        try {
            log.debug("음악 생성 상태 폴링 시작: taskId={}", taskId);
            
            // 30초 대기 후 첫 조회 (문서 상 최초 준비 시간 고려)
            TimeUnit.SECONDS.sleep(30);
            
            // 최대 10분 동안 15초 간격으로 재시도
            for (int attempt = 0; attempt < 40; attempt++) {
                try {
                    log.debug("상태 확인 시도 {}: taskId={}", attempt + 1, taskId);
                    
                    // 상태 확인
                    MusicGenerationResult result = sunoApiService.checkMusicStatus(taskId);
                    
                    if (result.getStatus() == MusicGenerationStatus.COMPLETED) {
                        handleMusicGenerationComplete(taskId, result);
                        log.info("음악 생성 완료: taskId={}", taskId);
                        return;
                    } else if (result.getStatus() == MusicGenerationStatus.FAILED) {
                        handleMusicGenerationFailed(taskId, result);
                        log.error("음악 생성 실패: taskId={}, error={}", taskId, result.getErrorMessage());
                        return;
                    } else if (result.getStatus() == MusicGenerationStatus.PROCESSING || result.getStatus() == MusicGenerationStatus.PENDING) {
                        // 아직 처리 중인 경우 - 계속 폴링
                        log.debug("음악 생성 진행 중: taskId={}, attempt={}", taskId, attempt + 1);
                    }
                    
                } catch (Exception e) {
                    // 일시적 오류인 경우 계속 재시도 (404 등)
                    log.warn("상태 확인 일시 오류 (재시도): taskId={}, attempt={}, msg={}", 
                            taskId, attempt + 1, e.getMessage());
                }
                
                // 마지막 시도가 아니면 15초 대기
                if (attempt < 39) {
                    TimeUnit.SECONDS.sleep(15);
                }
            }
            
            // 타임아웃 처리: 여전히 완료/실패 아님 → PROCESSING 유지
            log.warn("음악 생성 상태 확인 타임아웃: taskId={}", taskId);
            
        } catch (Exception e) {
            log.error("음악 생성 상태 폴링 실패: taskId={}", taskId, e);
            // 즉시 실패로 전환하지 않고 PROCESSING 유지 (콜백 대기 또는 다음 배치 확인)
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
            if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
                logoSong.setImageUrl(result.getImageUrl());
            }
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