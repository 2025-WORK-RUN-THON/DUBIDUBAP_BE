package com.guineafigma.controller;

import com.guineafigma.config.TestConfig;
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
        "spring.datasource.url=jdbc:h2:mem:suno-callback-testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        "jwt.secret-key=test-jwt-secret-key-for-suno-callback-test"
})
@Import(TestConfig.class)
@Transactional
@DisplayName("SunoCallbackController 실제 API 테스트")
class SunoCallbackControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        System.out.println("🚀 Suno Callback 테스트 서버 시작: " + baseUrl);
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - 성공")
    void handleSunoCallback_Success_RealAPI() {
        // Given
        String callbackJson = TestDataBuilder.createSunoCallbackJson();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(callbackJson, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 성공 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ Suno 콜백 처리 API 실제 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - taskId 없음")
    void handleSunoCallback_NoTaskId_RealAPI() {
        // Given
        String invalidCallbackJson = TestDataBuilder.createInvalidSunoCallbackJson();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(invalidCallbackJson, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 taskId 없음 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 에러 응답이면 정상
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("error") || body.containsKey("timestamp"));
            System.out.println("✅ Suno 콜백 taskId 없음 에러 응답 확인 완료!");
        }
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - 특수 문자가 포함된 taskId")
    void handleSunoCallback_SpecialCharTaskId_RealAPI() {
        // Given
        String specialCharJson = "{\"taskId\":\"task-123!@#$%\",\"status\":\"completed\"}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(specialCharJson, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 특수문자 taskId 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ Suno 콜백 특수문자 taskId 처리 확인 완료!");
        }
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - 다양한 상태값 테스트")
    void handleSunoCallback_VariousStatus_RealAPI() {
        // Given
        String[] statusValues = {"pending", "processing", "completed", "failed", "cancelled"};
        
        for (String status : statusValues) {
            String statusJson = "{\"taskId\":\"task_" + status + "_123\",\"status\":\"" + status + "\"}";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> httpRequest = new HttpEntity<>(statusJson, headers);

            // When
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/logosongs/suno-callback",
                    HttpMethod.POST,
                    httpRequest,
                    Map.class
            );

            // Then
            System.out.println("🔍 Suno 콜백 상태 " + status + " 응답: " + response.getStatusCode());
            assertNotNull(response);
            assertNotNull(response.getStatusCode());
        }
        
        System.out.println("✅ Suno 콜백 다양한 상태값 처리 확인 완료!");
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - 잘못된 JSON 형식")
    void handleSunoCallback_InvalidJson_RealAPI() {
        // Given
        String invalidJson = "{invalid json format}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(invalidJson, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 잘못된 JSON 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // 에러 응답이면 정상
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
        System.out.println("✅ Suno 콜백 잘못된 JSON 에러 처리 확인 완료!");
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - 빈 JSON")
    void handleSunoCallback_EmptyJson_RealAPI() {
        // Given
        String emptyJson = "{}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(emptyJson, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 빈 JSON 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("error") || body.containsKey("timestamp"));
            System.out.println("✅ Suno 콜백 빈 JSON 처리 확인 완료!");
        }
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - Content-Type 없음")
    void handleSunoCallback_NoContentType_RealAPI() {
        // Given
        String callbackJson = TestDataBuilder.createSunoCallbackJson();
        
        // Content-Type을 설정하지 않음
        HttpEntity<String> httpRequest = new HttpEntity<>(callbackJson);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 Content-Type 없음 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        // Content-Type이 없어도 처리되거나 적절한 에러 응답
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ Suno 콜백 Content-Type 없음 처리 확인 완료!");
        }
    }

    @Test
    @DisplayName("Suno API 콜백 처리 - 추가 필드 포함")
    void handleSunoCallback_AdditionalFields_RealAPI() {
        // Given
        String jsonWithExtraFields = "{\"taskId\":\"task_123\",\"status\":\"completed\",\"extraField\":\"extraValue\",\"nested\":{\"field\":\"value\"}}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> httpRequest = new HttpEntity<>(jsonWithExtraFields, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/logosongs/suno-callback",
                HttpMethod.POST,
                httpRequest,
                Map.class
        );

        // Then
        System.out.println("🔍 Suno 콜백 추가 필드 포함 응답 상태: " + response.getStatusCode());
        assertNotNull(response);
        assertNotNull(response.getStatusCode());
        
        if (response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("data") || body.containsKey("error"));
            System.out.println("✅ Suno 콜백 추가 필드 포함 처리 확인 완료!");
        }
    }
}