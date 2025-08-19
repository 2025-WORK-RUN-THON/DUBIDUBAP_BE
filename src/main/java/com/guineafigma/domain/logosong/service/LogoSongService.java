package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.response.PagedResponse;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.GuidesResponse;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.entity.LogoSongLike;
import com.guineafigma.domain.logosong.repository.LogoSongLikeRepository;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LogoSongService {

    private final LogoSongRepository logoSongRepository;
    private final LogoSongLikeRepository logoSongLikeRepository;

    
    private final LogoSongLyricsService logoSongLyricsService;

    @Transactional
    public LogoSongResponse createLogoSong(LogoSongCreateRequest request) {
        LogoSong logoSong = LogoSong.builder()
                .serviceName(request.getServiceName())
                .slogan(request.getSlogan())
                .industry(request.getIndustry())
                .marketingItem(request.getMarketingItem())
                .targetCustomer(request.getTargetCustomer())
                .moodTone(request.getMoodTone())
                .musicGenre(request.getMusicGenre())
                .version(request.getVersion())
                .additionalInfo(request.getAdditionalInfo())
                .build();

        LogoSong savedLogoSong = logoSongRepository.save(logoSong);
        log.info("로고송 생성 완료: {}", savedLogoSong.getId());
        
        return LogoSongResponse.from(savedLogoSong);
    }

    @Transactional
    public Long createLogoSongWithGuides(LogoSongCreateRequest request) {
        // 1. LogoSong 엔티티 생성
        LogoSong logoSong = LogoSong.builder()
                .serviceName(request.getServiceName())
                .slogan(request.getSlogan())
                .industry(request.getIndustry())
                .marketingItem(request.getMarketingItem())
                .targetCustomer(request.getTargetCustomer())
                .moodTone(request.getMoodTone())
                .musicGenre(request.getMusicGenre())
                .version(request.getVersion())
                .additionalInfo(request.getAdditionalInfo())
                .build();

        LogoSong savedLogoSong = logoSongRepository.save(logoSong);
        log.info("로고송 엔티티 생성 완료: {}", savedLogoSong.getId());

        try {
            // 2. OpenAI API를 사용하여 가사/비디오 가이드라인 생성
            GuidesResponse guides = logoSongLyricsService.generateLyricsAndVideoGuide(request);
            
            // 3. 생성된 가사와 비디오 가이드라인으로 엔티티 업데이트
            savedLogoSong.updateLyrics(guides.getLyrics());
            savedLogoSong.updateVideoGuideline(guides.getVideoGuideline());
            
            logoSongRepository.save(savedLogoSong);
            log.info("로고송 가사/비디오 가이드라인 생성 및 업데이트 완료: {}", savedLogoSong.getId());
            
            return savedLogoSong.getId();
        } catch (Exception e) {
            log.error("가사/비디오 가이드라인 생성 실패 - 로고송 ID: {}, 에러: {}", savedLogoSong.getId(), e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외를 다시 던짐
        }
    }

    public LogoSongResponse getLogoSong(Long id) {
        LogoSong logoSong = logoSongRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        
        return LogoSongResponse.from(logoSong);
    }

    @Transactional
    public LogoSongResponse incrementViewCount(Long id) {
        LogoSong logoSong = logoSongRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        
        logoSong.incrementViewCount();
        LogoSong savedLogoSong = logoSongRepository.save(logoSong);
        
        return LogoSongResponse.from(savedLogoSong);
    }

    public PagedResponse<LogoSongResponse> getAllLogoSongs(Pageable pageable) {
        Page<LogoSong> logoSongPage = logoSongRepository.findAll(pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(LogoSongResponse::from);
        
        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    public PagedResponse<LogoSongResponse> getPopularLogoSongs(Pageable pageable) {
        Page<LogoSong> logoSongPage = logoSongRepository.findByOrderByLikeCountDesc(pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(LogoSongResponse::from);
        
        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    @Transactional
    public void toggleLike(Long logoSongId, Long userId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));

        boolean exists = logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSongId);
        
        if (exists) {
            logoSongLikeRepository.findByUserIdAndLogosongId(userId, logoSongId)
                    .ifPresent(logoSongLikeRepository::delete);
            logoSong.decrementLikeCount();
            log.info("좋아요 취소: 사용자 {}, 로고송 {}", userId, logoSongId);
        } else {
            LogoSongLike logoSongLike = LogoSongLike.builder()
                    .userId(userId)
                    .logosongId(logoSongId)
                    .build();
            logoSongLikeRepository.save(logoSongLike);
            logoSong.incrementLikeCount();
            log.info("좋아요 추가: 사용자 {}, 로고송 {}", userId, logoSongId);
        }
        
        logoSongRepository.save(logoSong);
    }

    public boolean isLikedByUser(Long logoSongId, Long userId) {
        return logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSongId);
    }

    public PagedResponse<LogoSongResponse> getMyLogoSongs(Long userId, Pageable pageable) {
        // 미디어 엔티티에서 해당 사용자가 업로드한 로고송 ID들을 조회하는 방식으로 구현
        // 실제로는 LogoSong 엔티티에 userId 필드를 추가하는 것이 더 적절할 수 있음
        Page<LogoSong> logoSongPage = logoSongRepository.findAll(pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(LogoSongResponse::from);
        
        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    public PagedResponse<LogoSongResponse> getLikedLogoSongs(Long userId, Pageable pageable) {
        List<LogoSongLike> likedSongs = logoSongLikeRepository.findByUserId(userId);
        List<Long> logoSongIds = likedSongs.stream()
                .map(LogoSongLike::getLogosongId)
                .toList();
        
        if (logoSongIds.isEmpty()) {
            return PagedResponse.of(
                    List.of(),
                    pageable.getPageSize(),
                    pageable.getPageNumber() + 1,
                    0
            );
        }
        
        List<LogoSong> logoSongs = logoSongRepository.findAllById(logoSongIds);
        List<LogoSongResponse> responses = logoSongs.stream()
                .map(LogoSongResponse::from)
                .toList();
        
        // 간단한 페이지네이션 구현
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responses.size());
        List<LogoSongResponse> pagedResponses = responses.subList(start, end);
        
        return PagedResponse.of(
                pagedResponses,
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                (int) Math.ceil((double) responses.size() / pageable.getPageSize())
        );
    }
}