package com.guineafigma.domain.logosong.service;

import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.entity.LogoSongLike;
import com.guineafigma.domain.logosong.repository.LogoSongLikeRepository;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoSongService 단위 테스트")
class LogoSongServiceUnitTest {

    @Mock
    private LogoSongRepository logoSongRepository;

    @Mock
    private LogoSongLikeRepository logoSongLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LogoSongLyricsService logoSongLyricsService;

    @InjectMocks
    private LogoSongService logoSongService;

    private User testUser;
    private LogoSong testLogoSong;
    private LogoSongCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .nickname("testUser")
                .password("password123")
                .isActive(true)
                .build();
        testUser.setId(1L);

        testLogoSong = LogoSong.builder()
                .user(testUser)
                .serviceName("Test Service")
                .musicGenre("POP")
                .version(com.guineafigma.common.enums.VersionType.SHORT)
                .isPublic(true)
                .likeCount(0)
                .viewCount(0)
                .build();
        testLogoSong.setId(1L);

        createRequest = LogoSongCreateRequest.builder()
                .serviceName("Test Service")
                .musicGenre("POP")
                .version(com.guineafigma.common.enums.VersionType.SHORT)
                .build();
    }

    @Test
    @DisplayName("로고송 생성 성공")
    void createLogoSong_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(logoSongRepository.save(any(LogoSong.class))).thenReturn(testLogoSong);

        // when
        LogoSongResponse response = logoSongService.createLogoSong(createRequest, 1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Service", response.getServiceName());
        assertEquals("POP", response.getMusicGenre());
        assertEquals(1L, response.getUserId());
        assertEquals("testUser", response.getUserNickname());

        verify(userRepository).findById(1L);
        verify(logoSongRepository).save(any(LogoSong.class));
    }

    @Test
    @DisplayName("로고송 조회 성공")
    void getLogoSong_Success() {
        // given
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));

        // when
        LogoSongResponse response = logoSongService.getLogoSong(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Service", response.getServiceName());

        verify(logoSongRepository).findById(1L);
    }

    @Test
    @DisplayName("로고송 조회 실패 - 존재하지 않는 ID")
    void getLogoSong_Failure_NotFound() {
        // given
        when(logoSongRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> logoSongService.getLogoSong(999L));

        assertEquals(ErrorCode.LOGOSONG_NOT_FOUND, exception.getErrorCode());

        verify(logoSongRepository).findById(999L);
    }

    @Test
    @DisplayName("좋아요 토글 - 좋아요 추가")
    void toggleLike_AddLike() {
        // given
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));
        when(logoSongLikeRepository.existsByUserIdAndLogosongId(1L, 1L)).thenReturn(false);
        when(logoSongRepository.save(any(LogoSong.class))).thenReturn(testLogoSong);

        // when
        logoSongService.toggleLike(1L, 1L);

        // then
        verify(logoSongLikeRepository).save(any(LogoSongLike.class));
        verify(logoSongRepository).save(any(LogoSong.class));
        assertEquals(1, testLogoSong.getLikeCount());
    }

    @Test
    @DisplayName("좋아요 토글 - 좋아요 취소")
    void toggleLike_RemoveLike() {
        // given
        testLogoSong.setLikeCount(1);
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));
        when(logoSongLikeRepository.existsByUserIdAndLogosongId(1L, 1L)).thenReturn(true);

        LogoSongLike existingLike = LogoSongLike.builder()
                .userId(1L)
                .logosongId(1L)
                .build();
        when(logoSongLikeRepository.findByUserIdAndLogosongId(1L, 1L))
                .thenReturn(Optional.of(existingLike));
        when(logoSongRepository.save(any(LogoSong.class))).thenReturn(testLogoSong);

        // when
        logoSongService.toggleLike(1L, 1L);

        // then
        verify(logoSongLikeRepository).delete(existingLike);
        verify(logoSongRepository).save(any(LogoSong.class));
        assertEquals(0, testLogoSong.getLikeCount());
    }

    @Test
    @DisplayName("좋아요 토글 - 비공개 로고송 (소유자가 아닌 경우)")
    void toggleLike_PrivateSong_NonOwner() {
        // given
        testLogoSong.setIsPublic(false);
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> logoSongService.toggleLike(1L, 999L)); // 다른 사용자 ID

        assertEquals(ErrorCode.LOGOSONG_NOT_FOUND, exception.getErrorCode());

        verify(logoSongRepository).findById(1L);
        verify(logoSongLikeRepository, never()).save(any());
        verify(logoSongLikeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("좋아요 토글 - 비공개 로고송 (소유자인 경우)")
    void toggleLike_PrivateSong_Owner() {
        // given
        testLogoSong.setIsPublic(false);
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));
        when(logoSongLikeRepository.existsByUserIdAndLogosongId(1L, 1L)).thenReturn(false);
        when(logoSongRepository.save(any(LogoSong.class))).thenReturn(testLogoSong);

        // when
        logoSongService.toggleLike(1L, 1L); // 소유자 ID

        // then
        verify(logoSongLikeRepository).save(any(LogoSongLike.class));
        verify(logoSongRepository).save(any(LogoSong.class));
    }

    @Test
    @DisplayName("조회수 증가 성공")
    void incrementViewCount_Success() {
        // given
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));
        when(logoSongRepository.save(any(LogoSong.class))).thenReturn(testLogoSong);

        // when
        LogoSongResponse response = logoSongService.incrementViewCount(1L);

        // then
        assertNotNull(response);
        assertEquals(1, testLogoSong.getViewCount());

        verify(logoSongRepository).findById(1L);
        verify(logoSongRepository).save(any(LogoSong.class));
    }

    @Test
    @DisplayName("공개 로고송 목록 조회")
    void getAllLogoSongs_PublicOnly() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<LogoSong> logoSongs = Arrays.asList(testLogoSong);
        Page<LogoSong> page = new PageImpl<>(logoSongs, pageable, 1);

        when(logoSongRepository.findByIsPublicTrue(pageable)).thenReturn(page);

        // when
        var response = logoSongService.getAllLogoSongs(pageable);

        // then
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getId());

        verify(logoSongRepository).findByIsPublicTrue(pageable);
    }

    @Test
    @DisplayName("인기 로고송 목록 조회")
    void getPopularLogoSongs_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        testLogoSong.setLikeCount(10);
        List<LogoSong> logoSongs = Arrays.asList(testLogoSong);
        Page<LogoSong> page = new PageImpl<>(logoSongs, pageable, 1);

        when(logoSongRepository.findByOrderByLikeCountDesc(pageable)).thenReturn(page);

        // when
        var response = logoSongService.getPopularLogoSongs(pageable);

        // then
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(10, response.getContent().get(0).getLikeCount());

        verify(logoSongRepository).findByOrderByLikeCountDesc(pageable);
    }

    @Test
    @DisplayName("사용자별 좋아요 상태 포함 조회")
    void getLogoSongWithLike_Success() {
        // given
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));
        when(logoSongLikeRepository.existsByUserIdAndLogosongId(1L, 1L)).thenReturn(true);

        // when
        LogoSongResponse response = logoSongService.getLogoSongWithLike(1L, 1L);

        // then
        assertNotNull(response);
        assertTrue(response.isLiked());

        verify(logoSongRepository).findById(1L);
        verify(logoSongLikeRepository).existsByUserIdAndLogosongId(1L, 1L);
    }

    @Test
    @DisplayName("비공개 로고송 조회 - 소유자가 아닌 경우")
    void getLogoSongWithLike_PrivateSong_NonOwner() {
        // given
        testLogoSong.setIsPublic(false);
        when(logoSongRepository.findById(1L)).thenReturn(Optional.of(testLogoSong));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> logoSongService.getLogoSongWithLike(1L, 999L)); // 다른 사용자 ID

        assertEquals(ErrorCode.LOGOSONG_NOT_FOUND, exception.getErrorCode());

        verify(logoSongRepository).findById(1L);
    }

    @Test
    @DisplayName("좋아요 여부 확인")
    void isLikedByUser_Success() {
        // given
        when(logoSongLikeRepository.existsByUserIdAndLogosongId(1L, 1L)).thenReturn(true);

        // when
        boolean isLiked = logoSongService.isLikedByUser(1L, 1L);

        // then
        assertTrue(isLiked);

        verify(logoSongLikeRepository).existsByUserIdAndLogosongId(1L, 1L);
    }
}
