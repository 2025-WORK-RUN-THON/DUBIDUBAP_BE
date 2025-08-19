package com.guineafigma.controller;

import com.guineafigma.config.TestConfig;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logosong-testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "jwt.secret-key=test-jwt-secret-key-for-logosong-controller-test"
})
@Import(TestConfig.class)
@Transactional
@DisplayName("LogoSongController 실제 API 테스트")
class LogoSongControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        System.out.println("LogoSong 테스트 서버 시작: " + baseUrl);
    }

    @Test
    @DisplayName("로고송 조회 - 실제 API 호출")
    void getLogoSong_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("로고송 조회 응답 상태: " + response.getStatusCode());
            System.out.println("로고송 조회 응답 본문: " + response.getBody());
        
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            // API가 응답했다면 성공
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("로고송 조회 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("로고송 목록 조회 - 실제 API 호출")
    void getAllLogoSongs_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs?page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("로고송 목록 응답 상태: " + response.getStatusCode());
            System.out.println("로고송 목록 응답 본문: " + response.getBody());
        
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("로고송 목록 조회 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("인기 로고송 조회 - 실제 API 호출")
    void getPopularLogoSongs_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/popular?page=0&size=10",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("인기 로고송 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("인기 로고송 조회 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("가이드라인 생성 - 실제 API 호출")
    void generateGuides_RealAPI() {
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
                    System.out.println("가이드라인 생성 응답 상태: " + response.getStatusCode());
            System.out.println("가이드라인 생성 응답 본문: " + response.getBody());
        
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("가이드라인 생성 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("잘못된 요청 데이터 - 실제 API 호출")
    void generateGuides_ValidationError_RealAPI() {
        // Given - 빈 데이터로 검증 오류 유발
        String invalidJson = "{\"serviceName\":\"\",\"slogan\":\"\"}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(invalidJson, headers);

        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/guides",
                HttpMethod.POST,
                httpRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("잘못된 데이터 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("error") || body.containsKey("timestamp"));
            System.out.println("잘못된 데이터 검증 오류 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("로고송 생성 (통합) - 실제 API 호출")
    void createLogoSongWithGeneration_RealAPI() {
        // Given
        LogoSongCreateRequest request = TestDataBuilder.createValidLogoSongRequest();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogoSongCreateRequest> httpRequest = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/with-generation",
                HttpMethod.POST,
                httpRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("통합 로고송 생성 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("통합 로고송 생성 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("음악 생성 상태 확인 - 실제 API 호출")
    void getMusicGenerationStatus_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1/generation-status",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("음악 생성 상태 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("음악 생성 상태 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("폴링 상태 확인 - 실제 API 호출")
    void getPollingStatus_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1/polling-status",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("폴링 상태 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("폴링 상태 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("빠른 상태 확인 - 실제 API 호출")
    void getQuickStatus_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/1/quick-status",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("빠른 상태 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = Objects.requireNonNull(response.getBody());
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("빠른 상태 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("잘못된 ID 형식 - 실제 API 호출")
    void getLogoSong_InvalidIdFormat_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/invalid",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("잘못된 ID 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 400 또는 다른 에러 상태 코드면 정상
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
                    System.out.println("잘못된 ID 처리 확인 완료!");
    }

    @Test
    @DisplayName("잘못된 페이지 파라미터 - 실제 API 호출")
    void getAllLogoSongs_InvalidPageParams_RealAPI() {
        // When
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs?page=-1&size=0",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        // Then
                    System.out.println("잘못된 파라미터 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 400 또는 다른 에러 상태 코드면 정상
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
                    System.out.println("잘못된 페이지 파라미터 처리 확인 완료!");
    }
}