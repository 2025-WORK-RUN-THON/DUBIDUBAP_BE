package com.guineafigma.domain.logosong.service;

import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("LogoSongService 캐시 동작 테스트")
class LogoSongServiceCacheTest {

    @Autowired
    private LogoSongService logoSongService;

    @SpyBean
    private LogoSongRepository logoSongRepository;

    private Long logoSongId;

    @BeforeEach
    void setUp() {
        LogoSongCreateRequest req = TestDataBuilder.createValidLogoSongRequest();
        LogoSongResponse created = logoSongService.createLogoSong(req);
        logoSongId = created.getId();
        // 초기화: 호출 카운트 명확히 하기 위해
        Mockito.clearInvocations(logoSongRepository);
    }

    @Test
    @DisplayName("단건 조회 캐시 - 두 번째 호출부터 캐시 적중")
    void getLogoSong_CachedOnSecondCall() {
        // when
        LogoSongResponse first = logoSongService.getLogoSong(logoSongId);
        LogoSongResponse second = logoSongService.getLogoSong(logoSongId);

        // then
        assertThat(first.getId()).isEqualTo(second.getId());
        verify(logoSongRepository, times(1)).findById(logoSongId);
    }

    @Test
    @DisplayName("목록 조회 캐시 - 동일 페이지 파라미터는 두 번째 호출부터 캐시 적중")
    void getAllLogoSongs_ListCached() {
        // given: 데이터 더 생성
        for (int i = 0; i < 3; i++) {
            logoSongService.createLogoSong(TestDataBuilder.createValidLogoSongRequest());
        }
        Mockito.clearInvocations(logoSongRepository);

        var pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // when
        logoSongService.getAllLogoSongs(pageable);
        logoSongService.getAllLogoSongs(pageable);

        // then
        verify(logoSongRepository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("좋아요 토글 시 캐시 무효화 - 이후 단건 조회는 DB 재조회")
    void toggleLike_EvictsCaches() {
        // given: 단건 캐시 적중 상태를 만들기
        logoSongService.getLogoSong(logoSongId);
        logoSongService.getLogoSong(logoSongId);
        verify(logoSongRepository, times(1)).findById(logoSongId);
        Mockito.clearInvocations(logoSongRepository);

        // when: 좋아요 토글 → 캐시 무효화
        logoSongService.toggleLike(logoSongId, 123L);

        // then: toggleLike 내부에서 1회, 이후 조회에서 1회 → 총 2회
        logoSongService.getLogoSong(logoSongId);
        verify(logoSongRepository, times(2)).findById(logoSongId);
    }
}


