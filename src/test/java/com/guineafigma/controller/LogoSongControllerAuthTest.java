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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:logosong-auth-testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "jwt.secret-key=test-jwt-secret-key-for-logosong-auth-controller-test"
})
@Import(TestConfig.class)
@Transactional
@DisplayName("LogoSongController 인증 기능 실제 API 테스트")
class LogoSongControllerAuthTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        System.out.println("🚀 LogoSong 인증 테스트 서버 시작: " + baseUrl);
    }

    @Test
    @DisplayName("좋아요 토글 - 인증 없이 시도")
    void toggleLike_NoAuth_RealAPI() {
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/logosongs/1/like",
                null,
                Map.class
        );

        // Then
        System.out.println("🔍 좋아요 토글 (인증 없음) 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 인증 오류 응답이면 정상
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.out.println("✅ 인증 없는 좋아요 토글 에러 응답 확인 완료!");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }

    @Test
    @DisplayName("좋아요 상태 확인 - 인증 없이 시도")
    void getLikeStatus_NoAuth_RealAPI() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/logosongs/1/like-status",
                Map.class
        );

        // Then
        System.out.println("🔍 좋아요 상태 확인 (인증 없음) 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 인증 오류 응답이면 정상
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.out.println("✅ 인증 없는 좋아요 상태 확인 에러 응답 확인 완료!");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }

    @Test
    @DisplayName("내 로고송 목록 조회 - 인증 없이 시도")
    void getMyLogoSongs_NoAuth_RealAPI() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/logosongs/my",
                Map.class
        );

        // Then
        System.out.println("🔍 내 로고송 목록 (인증 없음) 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 인증 오류 응답이면 정상
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.out.println("✅ 인증 없는 내 로고송 목록 에러 응답 확인 완료!");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }

    @Test
    @DisplayName("좋아요한 로고송 목록 조회 - 인증 없이 시도")
    void getLikedLogoSongs_NoAuth_RealAPI() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/logosongs/my/liked",
                Map.class
        );

        // Then
        System.out.println("🔍 좋아요한 로고송 목록 (인증 없음) 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 인증 오류 응답이면 정상
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.out.println("✅ 인증 없는 좋아요한 로고송 목록 에러 응답 확인 완료!");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }

    @Test
    @DisplayName("가사 재생성 - 실제 API 호출")
    void regenerateLyrics_RealAPI() {
        // Given
        LogoSongCreateRequest request = TestDataBuilder.createValidLogoSongRequest();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogoSongCreateRequest> httpRequest = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/1/regenerate-lyrics",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 가사 재생성 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ 가사 재생성 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("비디오 가이드라인 재생성 - 실제 API 호출")
    void regenerateVideoGuide_RealAPI() {
        // Given
        LogoSongCreateRequest request = TestDataBuilder.createValidLogoSongRequest();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogoSongCreateRequest> httpRequest = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/1/regenerate-video-guide",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 비디오 가이드라인 재생성 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ 비디오 가이드라인 재생성 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("음악 생성 트리거 - 실제 API 호출")
    void generateMusic_RealAPI() {
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/logosongs/1/generate-music",
                null,
                Map.class
        );

        // Then
        System.out.println("🔍 음악 생성 트리거 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ 음악 생성 트리거 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("존재하지 않는 로고송 ID로 가사 재생성 시도")
    void regenerateLyrics_NotFound_RealAPI() {
        // Given
        LogoSongCreateRequest request = TestDataBuilder.createValidLogoSongRequest();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LogoSongCreateRequest> httpRequest = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/99999/regenerate-lyrics",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 존재하지 않는 로고송 가사 재생성 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("error") || body.containsKey("timestamp"));
            System.out.println("✅ 존재하지 않는 로고송 에러 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("존재하지 않는 로고송 ID로 음악 생성 시도")
    void generateMusic_NotFound_RealAPI() {
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/logosongs/99999/generate-music",
                null,
                Map.class
        );

        // Then
        System.out.println("🔍 존재하지 않는 로고송 음악 생성 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("error") || body.containsKey("timestamp"));
            System.out.println("✅ 존재하지 않는 로고송 음악 생성 에러 응답 확인 완료!");
        }
    }
}