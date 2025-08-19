package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("Polling/QuickStatus 캐시 동작 테스트")
class MusicGenerationPollingCacheTest {

    @Autowired
    private MusicGenerationPollingService pollingService;
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

        // 상태 값 세팅
        LogoSong song = logoSongRepository.findById(logoSongId).orElseThrow();
        song.updateMusicStatus(MusicGenerationStatus.PROCESSING);
        logoSongRepository.save(song);
        Mockito.clearInvocations(logoSongRepository);
    }

    @Test
    @DisplayName("QuickStatus 캐시 - TTL 내 두 번째 호출은 DB 조회 없음")
    void quickStatus_CachedWithinTtl() {
        pollingService.getQuickPollingStatus(logoSongId);
        pollingService.getQuickPollingStatus(logoSongId);

        verify(logoSongRepository, times(1)).findById(logoSongId);
    }
}


