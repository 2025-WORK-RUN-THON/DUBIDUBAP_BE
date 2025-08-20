package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.response.PagedResponse;
import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.GuidesResponse;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.entity.LogoSongLike;
import com.guineafigma.domain.logosong.repository.LogoSongLikeRepository;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoSongService {

    private final LogoSongRepository logoSongRepository;
    private final LogoSongLikeRepository logoSongLikeRepository;

    
    private final LogoSongLyricsService logoSongLyricsService;
    private final UserRepository userRepository;

    @Transactional
    @CacheEvict(value = {"logosong:list", "logosong:popular"}, allEntries = true)
    public LogoSongResponse createLogoSong(LogoSongCreateRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LogoSong logoSong = LogoSong.builder()
                .user(user)
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
        log.info("로고송 생성 완료: id={}, userId={}", savedLogoSong.getId(), userId);
        
        return LogoSongResponse.from(savedLogoSong);
    }

    // 테스트 및 기존 코드 호환을 위한 오버로드 (기본 사용자 생성/재사용)
    @Transactional
    @CacheEvict(value = {"logosong:list", "logosong:popular"}, allEntries = true)
    public LogoSongResponse createLogoSong(LogoSongCreateRequest request) {
        // 닉네임 'testUser' 사용자를 찾거나 생성
        User user = userRepository.findByNickname("testUser")
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .nickname("testUser")
                                .password("password")
                                .isActive(true)
                                .build()
                ));
        return createLogoSong(request, user.getId());
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular", "logosong:quickStatus"}, allEntries = true)
    public LogoSongResponse updateLyricsAndVideoGuide(Long logoSongId, String lyrics, String videoGuideline, Long userId) {
        LogoSong logoSong = logoSongRepository.findByIdAndUser_Id(logoSongId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateLyrics(lyrics);
        logoSong.updateVideoGuideline(videoGuideline);
        LogoSong saved = logoSongRepository.save(logoSong);
        return LogoSongResponse.from(saved);
    }

    // 호환용 오버로드 (소유자 검증 없이 동작) - 테스트 코드 호환 목적
    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular", "logosong:quickStatus"}, allEntries = true)
    public LogoSongResponse updateLyricsAndVideoGuide(Long logoSongId, String lyrics, String videoGuideline) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateLyrics(lyrics);
        logoSong.updateVideoGuideline(videoGuideline);
        LogoSong saved = logoSongRepository.save(logoSong);
        return LogoSongResponse.from(saved);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular", "logosong:quickStatus", "suno:status"}, allEntries = true)
    public void setMusicStatus(Long logoSongId, MusicGenerationStatus status) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateMusicStatus(status);
        logoSongRepository.save(logoSong);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular", "logosong:quickStatus"}, allEntries = true)
    public LogoSongResponse updateLyricsOnlyAndSetPending(Long logoSongId, String lyrics, Long userId) {
        LogoSong logoSong = logoSongRepository.findByIdAndUser_Id(logoSongId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateLyrics(lyrics);
        // 가사만 생성/재생성 시에는 음악 생성 워크플로우를 시작하지 않으므로 상태를 변경하지 않음(또는 null로 클리어)
        logoSong.updateMusicStatus(null);
        LogoSong saved = logoSongRepository.save(logoSong);
        return LogoSongResponse.from(saved);
    }

    // 호환용 오버로드
    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular", "logosong:quickStatus"}, allEntries = true)
    public LogoSongResponse updateLyricsOnlyAndSetPending(Long logoSongId, String lyrics) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateLyrics(lyrics);
        logoSong.updateMusicStatus(null);
        LogoSong saved = logoSongRepository.save(logoSong);
        return LogoSongResponse.from(saved);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public LogoSongResponse updateVideoGuidelineOnly(Long logoSongId, String videoGuideline, Long userId) {
        LogoSong logoSong = logoSongRepository.findByIdAndUser_Id(logoSongId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateVideoGuideline(videoGuideline);
        LogoSong saved = logoSongRepository.save(logoSong);
        return LogoSongResponse.from(saved);
    }

    // 호환용 오버로드
    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public LogoSongResponse updateVideoGuidelineOnly(Long logoSongId, String videoGuideline) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.updateVideoGuideline(videoGuideline);
        LogoSong saved = logoSongRepository.save(logoSong);
        return LogoSongResponse.from(saved);
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

    @Transactional(readOnly = true)
    @Cacheable(value = "logosong:byId", key = "#id", sync = true)
    public LogoSongResponse getLogoSong(Long id) {
        LogoSong logoSong = logoSongRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        
        return LogoSongResponse.from(logoSong);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public LogoSongResponse incrementViewCount(Long id) {
        LogoSong logoSong = logoSongRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        
        logoSong.incrementViewCount();
        LogoSong savedLogoSong = logoSongRepository.save(logoSong);
        
        return LogoSongResponse.from(savedLogoSong);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "logosong:list", key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true)
    public PagedResponse<LogoSongResponse> getAllLogoSongs(Pageable pageable) {
        Page<LogoSong> logoSongPage = logoSongRepository.findByIsPublicTrue(pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(LogoSongResponse::from);
        
        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "logosong:popular", key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true)
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

    @Transactional(readOnly = true)
    @Cacheable(value = "logosong:list", key = "'u:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true)
    public PagedResponse<LogoSongResponse> getAllLogoSongs(Pageable pageable, Long userId) {
        // 로그인 여부와 무관하게, 일반 목록은 공개된(isPublic=true) 로고송만 조회
        Page<LogoSong> logoSongPage = logoSongRepository.findByIsPublicTrue(pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(logoSong -> {
            Boolean liked = null;
            if (userId != null) {
                liked = logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSong.getId());
            }
            return LogoSongResponse.from(logoSong, liked);
        });

        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "logosong:popular", key = "'u:' + #userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort", sync = true)
    public PagedResponse<LogoSongResponse> getPopularLogoSongs(Pageable pageable, Long userId) {
        Page<LogoSong> logoSongPage = logoSongRepository.findByOrderByLikeCountDesc(pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(logoSong -> {
            Boolean liked = null;
            if (userId != null) {
                liked = logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSong.getId());
            }
            return LogoSongResponse.from(logoSong, liked);
        });

        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public LogoSongResponse getLogoSongWithLike(Long id, Long userId) {
        LogoSong logoSong = logoSongRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        // 비공개 로고송은 소유자만 접근 가능. 그 외에는 존재를 숨긴다(404)
        if (!Boolean.TRUE.equals(logoSong.getIsPublic())) {
            if (userId == null || !logoSong.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND);
            }
        }
        Boolean liked = null;
        if (userId != null) {
            liked = logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSong.getId());
        }
        return LogoSongResponse.from(logoSong, liked);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public LogoSongResponse incrementViewCountWithLike(Long id, Long userId) {
        LogoSong logoSong = logoSongRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        // 비공개 로고송은 소유자만 조회 가능. 그 외에는 존재를 숨긴다(404)
        if (!Boolean.TRUE.equals(logoSong.getIsPublic())) {
            if (userId == null || !logoSong.getUser().getId().equals(userId)) {
                throw new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND);
            }
        }

        logoSong.incrementViewCount();
        LogoSong savedLogoSong = logoSongRepository.save(logoSong);

        boolean liked = false;
        if (userId != null) {
            liked = logoSongLikeRepository.existsByUserIdAndLogosongId(userId, id);
        }

        return LogoSongResponse.from(savedLogoSong, liked);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
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

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public void like(Long logoSongId, Long userId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        boolean exists = logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSongId);
        if (exists) {
            return; // 멱등
        }
        LogoSongLike logoSongLike = LogoSongLike.builder()
                .userId(userId)
                .logosongId(logoSongId)
                .build();
        logoSongLikeRepository.save(logoSongLike);
        logoSong.incrementLikeCount();
        logoSongRepository.save(logoSong);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public void unlike(Long logoSongId, Long userId) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSongLikeRepository.findByUserIdAndLogosongId(userId, logoSongId)
                .ifPresent(like -> {
                    logoSongLikeRepository.delete(like);
                    logoSong.decrementLikeCount();
                });
        logoSongRepository.save(logoSong);
    }

    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long logoSongId, Long userId) {
        return logoSongLikeRepository.existsByUserIdAndLogosongId(userId, logoSongId);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public void updateVisibility(Long logoSongId, boolean publicVisible, String introduction, Long userId) {
        LogoSong logoSong = logoSongRepository.findByIdAndUser_Id(logoSongId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.setVisibility(publicVisible);
        if (introduction != null) {
            logoSong.updateIntroduction(introduction);
        }
        logoSongRepository.save(logoSong);
    }

    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public void updatePartial(Long logoSongId, Boolean isPublic, String introduction, Long userId) {
        LogoSong logoSong = logoSongRepository.findByIdAndUser_Id(logoSongId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        if (isPublic != null) {
            logoSong.setVisibility(isPublic);
        }
        if (introduction != null) {
            logoSong.updateIntroduction(introduction);
        }
        logoSongRepository.save(logoSong);
    }

    // 호환용 오버로드
    @Transactional
    @CacheEvict(value = {"logosong:byId", "logosong:list", "logosong:popular"}, allEntries = true)
    public void updateVisibility(Long logoSongId, boolean publicVisible) {
        LogoSong logoSong = logoSongRepository.findById(logoSongId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGOSONG_NOT_FOUND));
        logoSong.setVisibility(publicVisible);
        logoSongRepository.save(logoSong);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LogoSongResponse> getMyLogoSongs(Long userId, Pageable pageable) {
        // 미디어 엔티티에서 해당 사용자가 업로드한 로고송 ID들을 조회하는 방식으로 구현
        // 실제로는 LogoSong 엔티티에 userId 필드를 추가하는 것이 더 적절할 수 있음
        Page<LogoSong> logoSongPage = logoSongRepository.findByUser_Id(userId, pageable);
        Page<LogoSongResponse> responsePage = logoSongPage.map(LogoSongResponse::from);
        
        return PagedResponse.of(
                responsePage.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber() + 1,
                responsePage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
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