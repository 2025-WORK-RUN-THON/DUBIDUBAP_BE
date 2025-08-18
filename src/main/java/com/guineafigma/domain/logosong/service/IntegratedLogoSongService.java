package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.GuidesResponse;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IntegratedLogoSongService {

    private final LogoSongRepository logoSongRepository;
    private final LogoSongService logoSongService;
    private final LogoSongLyricsService logoSongLyricsService;
    private final SunoApiService sunoApiService;
    private final LogoSongGenerationService logoSongGenerationService;

    /**
     * 로고송 생성 - 가사/비디오 가이드라인 생성 + 음악 생성 통합 워크플로우
     */
    public LogoSongResponse createLogoSongWithGeneration(LogoSongCreateRequest request) {
        try {
            log.info("통합 로고송 생성 시작: serviceName={}", request.getServiceName());
            
            // 1. 기본 LogoSong 생성
            LogoSongResponse logoSongResponse = logoSongService.createLogoSong(request);
            LogoSong logoSong = logoSongRepository.findById(logoSongResponse.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
            
            // 2. 가사/비디오 가이드라인 생성 (기존 Assistant 기능)
            GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);
            
            // 3. LogoSong에 가사/가이드라인 저장
            logoSong.updateLyrics(guides.getLyrics());
            logoSong.updateVideoGuideline(guides.getVideoGuideline());
            logoSongRepository.save(logoSong);
            
            // 4. 음악 생성 상태를 PROCESSING으로 변경
            logoSong.updateMusicStatus(MusicGenerationStatus.PROCESSING);
            logoSongRepository.save(logoSong);
            
            // 5. Suno API로 음악 생성 요청 (비동기)
            logoSongGenerationService.generateLogoSongAsync(logoSong.getId());
            
            // 6. 응답 생성 (가사와 비디오 가이드라인 포함)
            LogoSongResponse response = convertToResponse(logoSong);
            
            log.info("통합 로고송 생성 완료: logoSongId={}", logoSong.getId());
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("통합 로고송 생성 실패: serviceName={}", request.getServiceName(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 기존 로고송에 대해 음악 생성 트리거
     */
    public void triggerMusicGeneration(Long logoSongId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        if (logoSong.getMusicStatus() == MusicGenerationStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.MUSIC_GENERATION_IN_PROGRESS);
        }

        if (logoSong.getLyrics() == null || logoSong.getLyrics().isEmpty()) {
            throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
        }

        logoSongGenerationService.generateLogoSongAsync(logoSongId);
        log.info("음악 생성 트리거: logoSongId={}", logoSongId);
    }

    /**
     * 기존 로고송에 대해 가사/비디오 가이드라인 재생성
     */
    public GuidesResponse regenerateLyricsAndGuide(Long logoSongId, LogoSongCreateRequest request) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        // 가사/비디오 가이드라인 재생성
        GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);
        
        // 업데이트
        logoSong.updateLyrics(guides.getLyrics());
        logoSong.updateVideoGuideline(guides.getVideoGuideline());
        logoSong.updateMusicStatus(MusicGenerationStatus.PENDING); // 음악 재생성 필요
        logoSongRepository.save(logoSong);

        log.info("가사/비디오 가이드라인 재생성: logoSongId={}", logoSongId);
        return guides;
    }

    /**
     * 음악 생성 상태 확인
     */
    @Transactional(readOnly = true)
    public MusicGenerationResult checkMusicGenerationStatus(Long logoSongId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        if (logoSong.getSunoTaskId() == null) {
            return MusicGenerationResult.builder()
                    .status(logoSong.getMusicStatus())
                    .build();
        }

        try {
            // Suno API에서 최신 상태 확인
            MusicGenerationResult result = sunoApiService.checkMusicStatus(logoSong.getSunoTaskId());
            
            // DB 상태와 다르면 업데이트
            if (result.getStatus() != logoSong.getMusicStatus()) {
                logoSong.updateMusicStatus(result.getStatus());
                if (result.getStatus() == MusicGenerationStatus.COMPLETED && result.getAudioUrl() != null) {
                    logoSong.updateGeneratedMusicUrl(result.getAudioUrl());
                }
                logoSongRepository.save(logoSong);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("음악 생성 상태 확인 실패: logoSongId={}", logoSongId, e);
            return MusicGenerationResult.builder()
                    .status(logoSong.getMusicStatus())
                    .errorMessage("상태 확인 실패")
                    .build();
        }
    }

    /**
     * 생성된 음악 다운로드 URL 조회
     */
    @Transactional(readOnly = true)
    public String getMusicDownloadUrl(Long logoSongId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        if (logoSong.getMusicStatus() != MusicGenerationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.MUSIC_GENERATION_FAILED);
        }

        if (logoSong.getGeneratedMusicUrl() == null) {
            throw new BusinessException(ErrorCode.MUSIC_GENERATION_FAILED);
        }

        return logoSong.getGeneratedMusicUrl();
    }

    private LogoSongResponse convertToResponse(LogoSong logoSong) {
        return LogoSongResponse.builder()
                .id(logoSong.getId())
                .serviceName(logoSong.getServiceName())
                .slogan(logoSong.getSlogan())
                .industry(logoSong.getIndustry())
                .marketingItem(logoSong.getMarketingItem())
                .targetCustomer(logoSong.getTargetCustomer())
                .moodTone(logoSong.getMoodTone())
                .musicGenre(logoSong.getMusicGenre())
                .version(logoSong.getVersion())
                .additionalInfo(logoSong.getAdditionalInfo())
                .likeCount(logoSong.getLikeCount())
                .viewCount(logoSong.getViewCount())
                .lyrics(logoSong.getLyrics())
                .videoGuideline(logoSong.getVideoGuideline())
                .musicStatus(logoSong.getMusicStatus())
                .generatedMusicUrl(logoSong.getGeneratedMusicUrl())
                .createdAt(logoSong.getCreatedAt())
                .build();
    }
}