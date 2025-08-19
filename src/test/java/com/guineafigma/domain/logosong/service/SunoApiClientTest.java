package com.guineafigma.domain.logosong.service;

import com.guineafigma.config.TestConfig;
import com.guineafigma.domain.logosong.client.SunoApiClient;
import com.guineafigma.domain.logosong.dto.request.SunoGenerateRequest;
import com.guineafigma.domain.logosong.dto.response.SunoGenerateResponse;
import com.guineafigma.domain.logosong.dto.response.SunoStatusResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

//@Slf4j
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.cloud.aws.stack.auto=false",
    "spring.cloud.aws.region.auto=false",
    "cloud.aws.s3.bucket=test-bucket",
    "cloud.aws.credentials.access-key=test-key",
    "cloud.aws.credentials.secret-key=test-secret",
    "jwt.secret-key=test-jwt-secret-key-for-testing-purposes-only"
})
@Disabled("실제 Suno API 키가 필요한 통합 테스트 - 수동 실행 필요")
public class SunoApiClientTest {

    private static final Logger log = LoggerFactory.getLogger(SunoApiClientTest.class);

    @Autowired
    private SunoApiClient sunoApiClient;

    @Test
    void testGenerateMusic() {
        // Given
        SunoGenerateRequest request = SunoGenerateRequest.of(
                "Create a short, catchy jingle for a tech startup called 'TechFlow'. " +
                "The song should be upbeat, modern, and include the name 'TechFlow' at least twice. " +
                "Style: electronic, energetic, 30 seconds",
                "electronic, upbeat, commercial, tech, jingle",
                "TechFlow - Logo Song",
                "v3_5"
        );

        // When
        SunoGenerateResponse response = sunoApiClient.generateMusic(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        log.info("Suno API 음악 생성 요청 성공: taskId={}", response.getId());
        
        // 상태 확인 테스트
        testCheckMusicStatus(response.getId());
    }

    void testCheckMusicStatus(String taskId) {
        // Given
        assertNotNull(taskId);

        // When
        SunoStatusResponse response = sunoApiClient.getGenerationStatus(taskId);

        // Then
        assertNotNull(response);
        assertNotNull(response.getStatus());
        log.info("Suno API 상태 확인 성공: taskId={}, status={}", taskId, response.getStatus());
        
        // 상태별 추가 검증
        switch (response.getStatus()) {
            case "complete" -> {
                assertNotNull(response.getAudioUrl(), "완료된 작업은 오디오 URL이 있어야 함");
                assertNotNull(response.getDuration(), "완료된 작업은 duration이 있어야 함");
                log.info("음악 생성 완료: audioUrl={}, duration={}초", 
                        response.getAudioUrl(), response.getDuration());
            }
            case "error" -> {
                assertNotNull(response.getErrorMessage(), "실패한 작업은 에러 메시지가 있어야 함");
                log.error("음악 생성 실패: error={}", response.getErrorMessage());
            }
            default -> {
                log.info("음악 생성 진행 중: status={}", response.getStatus());
            }
        }
    }

    @Test
    void testGetRemainingCredits() {
        // When
        Double credits = sunoApiClient.getRemainingCredits();

        // Then
        log.info("Suno API 남은 크레딧: {}", credits);
        // credits가 null일 수 있음 (구현에 따라)
    }

    // 실제 음악 생성 플로우 테스트 (수동 실행 필요)
    @Test
    @Disabled("수동 실행 전용 - 실제 크레딧 소모")
    void testFullMusicGenerationFlow() throws InterruptedException {
        // 1. 음악 생성 요청
        SunoGenerateRequest request = SunoGenerateRequest.of(
                "Korean brand jingle for coffee shop called '카페 모닝'. " +
                "Warm, cozy atmosphere. Include '카페 모닝' twice. " +
                "Acoustic guitar, soft vocals, 30 seconds.",
                "acoustic, korean, coffee, warm, cozy",
                "카페 모닝 - 브랜드 징글",
                "v4"
        );

        SunoGenerateResponse generateResponse = sunoApiClient.generateMusic(request);
        String taskId = generateResponse.getId();
        log.info("음악 생성 시작: taskId={}", taskId);

        // 2. 상태 폴링 (최대 10분)
        int maxAttempts = 60; // 10분 (10초 간격)
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            Thread.sleep(10000); // 10초 대기
            attempts++;
            
            SunoStatusResponse statusResponse = sunoApiClient.getGenerationStatus(taskId);
            log.info("폴링 시도 {}/{}: status={}", attempts, maxAttempts, statusResponse.getStatus());
            
            if (statusResponse.isCompleted()) {
                log.info("음악 생성 완료!");
                log.info("Audio URL: {}", statusResponse.getAudioUrl());
                log.info("Duration: {}초", statusResponse.getDuration());
                return;
            } else if (statusResponse.isFailed()) {
                log.error("음악 생성 실패: {}", statusResponse.getErrorMessage());
                fail("음악 생성이 실패했습니다: " + statusResponse.getErrorMessage());
            }
        }
        
        fail("음악 생성이 시간 내에 완료되지 않았습니다.");
    }
}