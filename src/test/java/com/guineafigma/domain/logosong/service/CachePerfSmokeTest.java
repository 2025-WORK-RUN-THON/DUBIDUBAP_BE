package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("캐시 성능 스모크 테스트 (콘솔 출력)")
class CachePerfSmokeTest {

    @Autowired
    private LogoSongService logoSongService;

    @Autowired
    private MusicGenerationPollingService pollingService;

    @Test
    void measureColdWarmTimes() {
        // 데이터 준비
        Long id = logoSongService.createLogoSong(TestDataBuilder.createValidLogoSongRequest()).getId();
        for (int i = 0; i < 10; i++) {
            logoSongService.createLogoSong(adjustSvcName(TestDataBuilder.createValidLogoSongRequest(), i));
        }

        var pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // 목록
        long t1 = now(); logoSongService.getAllLogoSongs(pageable); long coldList = elapsed(t1);
        long t2 = now(); logoSongService.getAllLogoSongs(pageable); long warmList = elapsed(t2);
        System.out.println("[PERF] list cold(ms)=" + coldList + ", warm(ms)=" + warmList);

        // 단건
        long t3 = now(); logoSongService.getLogoSong(id); long coldOne = elapsed(t3);
        long t4 = now(); logoSongService.getLogoSong(id); long warmOne = elapsed(t4);
        System.out.println("[PERF] byId cold(ms)=" + coldOne + ", warm(ms)=" + warmOne);

        // quick status (DB만)
        // 초기 상태 null 방지: PENDING으로 설정
        logoSongService.setMusicStatus(id, MusicGenerationStatus.PENDING);
        long t5 = now(); pollingService.getQuickPollingStatus(id); long coldQuick = elapsed(t5);
        long t6 = now(); pollingService.getQuickPollingStatus(id); long warmQuick = elapsed(t6);
        System.out.println("[PERF] quickStatus cold(ms)=" + coldQuick + ", warm(ms)=" + warmQuick);
    }

    private static long now() { return System.nanoTime(); }
    private static long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }

    private static LogoSongCreateRequest adjustSvcName(LogoSongCreateRequest req, int idx) {
        return LogoSongCreateRequest.builder()
                .serviceName(req.getServiceName() + "-" + idx)
                .slogan(req.getSlogan())
                .industry(req.getIndustry())
                .marketingItem(req.getMarketingItem())
                .targetCustomer(req.getTargetCustomer())
                .moodTone(req.getMoodTone())
                .musicGenre(req.getMusicGenre())
                .version(req.getVersion())
                .additionalInfo(req.getAdditionalInfo())
                .build();
    }
}


