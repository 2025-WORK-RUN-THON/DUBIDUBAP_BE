package com.guineafigma.domain.logosong.service;

import com.guineafigma.common.enums.MusicGenre;
import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.common.enums.VersionType;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationStatusResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

//@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.cloud.aws.stack.auto=false",
    "spring.cloud.aws.region.auto=false",
    "cloud.aws.region.static=ap-northeast-2",
    "cloud.aws.s3.bucket=test-bucket",
    "cloud.aws.credentials.access-key=test-key",
    "cloud.aws.credentials.secret-key=test-secret",
    "jwt.secret-key=test-jwt-secret-key-for-testing-purposes-only"
})
@Transactional
public class IntegratedLogoSongServiceTest {

    private static final Logger log = LoggerFactory.getLogger(IntegratedLogoSongServiceTest.class);

    @Autowired
    private IntegratedLogoSongService integratedLogoSongService;

    @Autowired
    private MusicGenerationPollingService pollingService;

    @Autowired
    private LogoSongRepository logoSongRepository;

    @Test
    void testCreateLogoSongWithGeneration_WithoutSunoAPI() {
        // Given
        LogoSongCreateRequest request = createTestRequest();

        // When - Suno API 호출 없이 가사 생성까지만 테스트
        // 실제 환경에서는 @MockBean으로 SunoApiClient를 모킹해야 함
        LogoSongResponse response = integratedLogoSongService.createLogoSongWithGeneration(request);

        // Then
        assertNotNull(response);
        assertEquals("테스트 서비스", response.getServiceName());
        assertNotNull(response.getLyrics()); // 가사가 생성되어야 함
        assertNotNull(response.getVideoGuideline()); // 비디오 가이드라인이 생성되어야 함
        assertEquals(MusicGenerationStatus.PROCESSING, response.getMusicStatus()); // 음악 생성 진행 중

        log.info("로고송 생성 테스트 완료: id={}, lyrics length={}", 
                response.getId(), response.getLyrics().length());
    }

    @Test
    void testPollingStatus() {
        // Given - 테스트용 로고송 생성
        LogoSong logoSong = createTestLogoSong();
        logoSongRepository.save(logoSong);

        // When
        MusicGenerationStatusResponse status = pollingService.getQuickPollingStatus(logoSong.getId());

        // Then
        assertNotNull(status);
        assertEquals(logoSong.getId(), status.getLogoSongId());
        assertEquals(MusicGenerationStatus.PENDING, status.getStatus());
        assertNotNull(status.getStatusMessage());
        assertNotNull(status.getNextPollInterval());

        log.info("폴링 상태 테스트 완료: status={}, message={}", 
                status.getStatus(), status.getStatusMessage());
    }

    @Test
    @Disabled("실제 Suno API 호출 - 수동 실행 필요")
    void testFullIntegrationWithSunoAPI() throws InterruptedException {
        // Given
        LogoSongCreateRequest request = createTestRequest();

        // When - 실제 Suno API 호출
        LogoSongResponse response = integratedLogoSongService.createLogoSongWithGeneration(request);

        // Then - 초기 응답 검증
        assertNotNull(response);
        assertEquals(MusicGenerationStatus.PROCESSING, response.getMusicStatus());

        // 폴링으로 완료 확인 (최대 10분)
        Long logoSongId = response.getId();
        int maxAttempts = 60;
        int attempts = 0;

        while (attempts < maxAttempts) {
            Thread.sleep(10000); // 10초 대기
            attempts++;

            MusicGenerationStatusResponse status = pollingService.getPollingStatus(logoSongId);
            log.info("폴링 시도 {}/{}: status={}, progress={}%", 
                    attempts, maxAttempts, status.getStatus(), status.getProgress());

            if (status.getStatus() == MusicGenerationStatus.COMPLETED) {
                log.info("통합 테스트 성공!");
                log.info("Audio URL: {}", status.getAudioUrl());
                assertNotNull(status.getAudioUrl());
                return;
            } else if (status.getStatus() == MusicGenerationStatus.FAILED) {
                log.error("통합 테스트 실패: {}", status.getErrorMessage());
                fail("음악 생성이 실패했습니다: " + status.getErrorMessage());
            }
        }

        fail("통합 테스트 시간 초과");
    }

    private LogoSongCreateRequest createTestRequest() {
        return LogoSongCreateRequest.builder()
                .serviceName("테스트 서비스")
                .slogan("혁신적인 서비스")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre(MusicGenre.ELECTRONIC)
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송입니다.")
                .build();
    }

    private LogoSong createTestLogoSong() {
        return LogoSong.builder()
                .serviceName("테스트 서비스")
                .slogan("테스트 슬로건")
                .industry("IT")
                .musicGenre(MusicGenre.POP)
                .version(VersionType.SHORT)
                .lyrics("테스트 가사")
                .videoGuideline("테스트 비디오 가이드라인")
                .musicStatus(MusicGenerationStatus.PENDING)
                .likeCount(0)
                .viewCount(0)
                .build();
    }
}