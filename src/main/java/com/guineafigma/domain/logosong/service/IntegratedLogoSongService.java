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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegratedLogoSongService {

    private final LogoSongRepository logoSongRepository;
    private final LogoSongService logoSongService;
    private final LogoSongLyricsService logoSongLyricsService;
    private final SunoApiService sunoApiService;
    private final LogoSongGenerationService logoSongGenerationService;

    // 로고송 생성 - 가사/비디오 가이드라인 생성 + 음악 생성 통합 워크플로우
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LogoSongResponse createLogoSongWithGeneration(LogoSongCreateRequest request) {
        try {
            log.info("통합 로고송 생성 시작: serviceName={}", request.getServiceName());
            
            // 1. 기본 LogoSong 생성 (짧은 트랜잭션)
            LogoSongResponse created = logoSongService.createLogoSong(request);
            Long logoSongId = created.getId();

            // 2. 가사/비디오 가이드라인 생성 (트랜잭션 없음, 외부 API)
            GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);

            // 3. LogoSong에 가사/가이드라인 저장 (짧은 트랜잭션)
            LogoSongResponse updated = logoSongService.updateLyricsAndVideoGuide(
                    logoSongId, guides.getLyrics(), guides.getVideoGuideline());

            // 4. 트랜잭션 커밋 이후 비동기 음악 생성 트리거
            logoSongGenerationService.generateLogoSongAsync(logoSongId);

            log.info("통합 로고송 생성 완료: logoSongId={}", logoSongId);
            return updated;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("통합 로고송 생성 실패: serviceName={}", request.getServiceName(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 로고송 생성 - 가사/비디오 가이드라인만 생성하여 저장하고 전체 레코드를 반환
    // 음악 생성은 시작하지 않음
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LogoSongResponse createLogoSongWithGuidesOnly(LogoSongCreateRequest request) {
        try {
            log.info("로고송(가사만) 생성 시작: serviceName={}", request.getServiceName());

            // 1. 기본 LogoSong 생성 (짧은 트랜잭션)
            LogoSongResponse created = logoSongService.createLogoSong(request);

            // 2. 가사/비디오 가이드라인 생성 (트랜잭션 없음)
            GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);

            // 3. DB 업데이트 (짧은 트랜잭션) 후 전체 레코드 반환
            return logoSongService.updateLyricsAndVideoGuide(
                    created.getId(), guides.getLyrics(), guides.getVideoGuideline());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("로고송(가사만) 생성 실패: serviceName={}", request.getServiceName(), e);
            throw new BusinessException(ErrorCode.LYRICS_GENERATION_FAILED);
        }
    }

    // 기존 로고송에 대해 음악 생성 트리거
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

    // 기존 로고송에 대해 가사/비디오 가이드라인 재생성
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LogoSongResponse regenerateLyricsAndGuide(Long logoSongId, LogoSongCreateRequest request) {
        // 1) 외부 API 호출 (트랜잭션 없음)
        GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);
        // 2) DB 업데이트 (짧은 트랜잭션)
        LogoSongResponse updated = logoSongService.updateLyricsAndVideoGuide(
                logoSongId, guides.getLyrics(), guides.getVideoGuideline());
        // 3) 음악 재생성 필요 상태로 변경 (짧은 트랜잭션)
        logoSongService.setMusicStatus(logoSongId, MusicGenerationStatus.PENDING);

        log.info("가사/비디오 가이드라인 재생성: logoSongId={}", logoSongId);
        return updated;
    }

    // 가사만 재생성 (비디오 가이드라인은 유지)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LogoSongResponse regenerateLyricsOnly(Long logoSongId, LogoSongCreateRequest request) {
        GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);
        LogoSongResponse updated = logoSongService.updateLyricsOnlyAndSetPending(
                logoSongId, guides.getLyrics());
        log.info("가사 재생성 완료: logoSongId={}", logoSongId);
        return updated;
    }

    // 비디오 가이드라인만 (재)생성 - logosong id 기준
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LogoSongResponse regenerateVideoGuideOnly(Long logoSongId, LogoSongCreateRequest request) {
        GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);
        LogoSongResponse updated = logoSongService.updateVideoGuidelineOnly(
                logoSongId, guides.getVideoGuideline());
        log.info("비디오 가이드라인 (재)생성 완료: logoSongId={}", logoSongId);
        return updated;
    }

    // 음악 생성 상태 확인 (쓰기 없음, 트랜잭션 비활성화)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
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
            // 여기서는 DB 업데이트를 수행하지 않음 (롤백 마킹 방지)
            return result;
            
        } catch (Exception e) {
            log.error("음악 생성 상태 확인 실패: logoSongId={}", logoSongId, e);
            // 실패 시에도 현재 DB 상태 반환
            return MusicGenerationResult.builder()
                    .status(logoSong.getMusicStatus())
                    .errorMessage("상태 확인 실패: " + (e.getMessage() != null ? e.getMessage() : "unknown"))
                    .build();
        }
    }

    // 생성된 음악 다운로드 URL 조회
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
}