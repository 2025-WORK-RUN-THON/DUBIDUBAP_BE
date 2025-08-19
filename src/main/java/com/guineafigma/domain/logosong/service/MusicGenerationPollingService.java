package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationStatusResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicGenerationPollingService {

    private final LogoSongRepository logoSongRepository;
    private final SunoApiService sunoApiService;

    // 웹 클라이언트 폴링을 위한 최적화된 상태 확인
    @Transactional
    public MusicGenerationStatusResponse getPollingStatus(Long logoSongId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        try {
            // 1. 현재 DB 상태 확인
            MusicGenerationStatus currentStatus = logoSong.getMusicStatus();
            
            // 2. 이미 완료되었거나 실패한 경우 DB 정보 반환
            if (currentStatus == MusicGenerationStatus.COMPLETED) {
                return MusicGenerationStatusResponse.completed(
                        logoSong.getId(),
                        logoSong.getSunoTaskId(),
                        logoSong.getGeneratedMusicUrl(),
                        null, // 비디오 URL은 현재 미지원
                        null, // duration은 현재 미지원
                        logoSong.getCreatedAt(), // 시작 시간 대용
                        logoSong.getGeneratedAt()
                );
            }
            
            if (currentStatus == MusicGenerationStatus.FAILED) {
                return MusicGenerationStatusResponse.failed(
                        logoSong.getId(),
                        logoSong.getSunoTaskId(),
                        "음악 생성에 실패했습니다.",
                        logoSong.getCreatedAt()
                );
            }
            
            if (currentStatus == MusicGenerationStatus.PENDING) {
                return MusicGenerationStatusResponse.pending(logoSong.getId());
            }
            
            // 3. PROCESSING 상태인 경우 Suno API 확인
            if (currentStatus == MusicGenerationStatus.PROCESSING && logoSong.getSunoTaskId() != null) {
                try {
                    var result = sunoApiService.checkMusicStatus(logoSong.getSunoTaskId());
                    
                    // 상태가 변경된 경우 DB 업데이트
                    if (result.getStatus() != currentStatus) {
                        logoSong.updateMusicStatus(result.getStatus());
                        
                        if (result.getStatus() == MusicGenerationStatus.COMPLETED) {
                            logoSong.updateGeneratedMusicUrl(result.getAudioUrl());
                        }
                        
                        logoSongRepository.save(logoSong);
                        
                        log.info("음악 생성 상태 업데이트: logoSongId={}, {} -> {}", 
                                logoSong.getId(), currentStatus, result.getStatus());
                    }
                    
                    // 응답 생성
                    return switch (result.getStatus()) {
                        case COMPLETED -> MusicGenerationStatusResponse.completed(
                                logoSong.getId(),
                                logoSong.getSunoTaskId(),
                                result.getAudioUrl(),
                                result.getVideoUrl(),
                                result.getDuration(),
                                logoSong.getCreatedAt(),
                                LocalDateTime.now()
                        );
                        case FAILED -> MusicGenerationStatusResponse.failed(
                                logoSong.getId(),
                                logoSong.getSunoTaskId(),
                                result.getErrorMessage(),
                                logoSong.getCreatedAt()
                        );
                        default -> MusicGenerationStatusResponse.processing(
                                logoSong.getId(),
                                logoSong.getSunoTaskId(),
                                logoSong.getCreatedAt()
                        );
                    };
                    
                } catch (Exception e) {
                    log.error("Suno API 상태 확인 실패: logoSongId={}", logoSong.getId(), e);
                    // API 오류 시에도 진행 중으로 응답 (일시적 오류일 수 있음)
                    return MusicGenerationStatusResponse.processing(
                            logoSong.getId(),
                            logoSong.getSunoTaskId(),
                            logoSong.getCreatedAt()
                    );
                }
            }
            
            // 기본값: 진행 중
            return MusicGenerationStatusResponse.processing(
                    logoSong.getId(),
                    logoSong.getSunoTaskId(),
                    logoSong.getCreatedAt()
            );
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("폴링 상태 확인 중 예외 발생: logoSongId={}", logoSongId, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 폴링 상태 확인 (간단 버전)
    @Transactional(readOnly = true)
    public MusicGenerationStatusResponse getQuickPollingStatus(Long logoSongId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        // DB 상태만 확인 (Suno API 호출 없음)
        return switch (logoSong.getMusicStatus()) {
            case COMPLETED -> MusicGenerationStatusResponse.completed(
                    logoSong.getId(),
                    logoSong.getSunoTaskId(),
                    logoSong.getGeneratedMusicUrl(),
                    null,
                    null,
                    logoSong.getCreatedAt(),
                    logoSong.getGeneratedAt()
            );
            case FAILED -> MusicGenerationStatusResponse.failed(
                    logoSong.getId(),
                    logoSong.getSunoTaskId(),
                    "음악 생성에 실패했습니다.",
                    logoSong.getCreatedAt()
            );
            case PENDING -> MusicGenerationStatusResponse.pending(logoSong.getId());
            default -> MusicGenerationStatusResponse.processing(
                    logoSong.getId(),
                    logoSong.getSunoTaskId(),
                    logoSong.getCreatedAt()
            );
        };
    }

    // 폴링 만료된 작업들 정리
    @Transactional
    public void cleanupExpiredPolling() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(30);
        
        // 30분 이상 진행 중인 작업들을 FAILED로 변경
        var expiredLogoSongs = logoSongRepository.findByMusicStatus(MusicGenerationStatus.PROCESSING)
                .stream()
                .filter(logoSong -> logoSong.getUpdatedAt().isBefore(expiredTime))
                .toList();
        
        for (LogoSong logoSong : expiredLogoSongs) {
            logoSong.updateMusicStatus(MusicGenerationStatus.FAILED);
            logoSongRepository.save(logoSong);
            log.warn("폴링 만료로 작업 실패 처리: logoSongId={}", logoSong.getId());
        }
        
        if (!expiredLogoSongs.isEmpty()) {
            log.info("폴링 만료 작업 정리 완료: count={}", expiredLogoSongs.size());
        }
    }
}