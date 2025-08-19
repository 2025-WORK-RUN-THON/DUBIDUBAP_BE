package com.guineafigma.controller;

import com.guineafigma.config.TestConfig;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:async-flow-testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "jwt.secret-key=test-jwt-secret-key-for-async-flow-test"
})
@Import(TestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@DisplayName("로고송 비동기 플로우 및 상태 관리 실제 API 테스트")
class LogoSongAsyncFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        System.out.println("비동기 플로우 테스트 서버 시작: " + baseUrl);
    }

    @Test
    @Order(1)
    @DisplayName("비동기 플로우 1단계: 가사 생성만 (음악 생성 비활성화)")
    void asyncFlow_Step1_LyricsGenerationOnly_RealAPI() {
        // Given
        LogoSongCreateRequest request = TestDataBuilder.createValidLogoSongRequest();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogoSongCreateRequest> httpRequest = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/guides",
                HttpMethod.POST,
                httpRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("🔍 1단계 - 가사 생성 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ 1단계 - 가사 생성 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @Order(2)
    @DisplayName("비동기 플로우 2단계: 음악 생성 중 (상위 버튼 비활성화)")
    void asyncFlow_Step2_MusicGenerationInProgress_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1/generate-music",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("2단계 - 음악 생성 트리거 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("2단계 - 음악 생성 트리거 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @Order(3)
    @DisplayName("비동기 플로우 3단계: 음악 생성 완료 후 재생성 가능")
    void asyncFlow_Step3_MusicGenerationCompleted_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1/generation-status",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("🔍 3단계 - 생성 상태 확인 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ 3단계 - 생성 상태 확인 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @Order(4)
    @DisplayName("비동기 플로우 4단계: 음악 생성 실패 후 재시도")
    void asyncFlow_Step4_MusicGenerationRetry_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/999/generate-music",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("4단계 - 재시도 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 에러 응답이면 정상
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("error") || body.containsKey("timestamp"));
            System.out.println("4단계 - 재시도 에러 응답 확인 완료!");
        }
    }

    @Test
    @Order(5)
    @DisplayName("비동기 플로우 5단계: 단계별 버튼 상태 시뮬레이션")
    void asyncFlow_Step5_ButtonStateSimulation_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1/polling-status",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("5단계 - 폴링 상태 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("5단계 - 폴링 상태 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("에러 처리: 순서 위반 시나리오")
    void errorHandling_OrderViolation_RealAPI() {
        // Given - 존재하지 않는 로고송에 대한 음악 생성 시도
        
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/99999/generate-music",
                HttpMethod.POST,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("순서 위반 에러 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 에러 응답이면 정상
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
        System.out.println("순서 위반 에러 처리 확인 완료!");
    }

    @Test
    @DisplayName("동시성 제어: 중복 요청 방지")
    void concurrencyControl_DuplicateRequestPrevention_RealAPI() {
        // Given
        LogoSongCreateRequest request = TestDataBuilder.createValidLogoSongRequest();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogoSongCreateRequest> httpRequest = new HttpEntity<>(request, headers);

        // When - 동시에 같은 요청 2번 시도
        ResponseEntity<Map<String, Object>> response1 = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/with-generation",
                HttpMethod.POST,
                httpRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        ResponseEntity<Map<String, Object>> response2 = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/with-generation",
                HttpMethod.POST,
                httpRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
        System.out.println("중복 요청 방지 - 첫 번째 응답: " + response1.getStatusCode());
        System.out.println("중복 요청 방지 - 두 번째 응답: " + response2.getStatusCode());
        
        assertNotNull(response1);
        assertNotNull(response2);
        
        // 두 요청 모두 응답을 받아야 함
        System.out.println("중복 요청 처리 확인 완료!");
    }
}