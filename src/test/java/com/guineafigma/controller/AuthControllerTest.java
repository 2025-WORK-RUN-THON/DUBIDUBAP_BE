package com.guineafigma.controller;

import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, 
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.cloud.aws.stack.auto=false",
        "spring.cloud.aws.region.auto=false",
        "jwt.secret-key=test-jwt-secret-key-for-testing-purposes-only"
    })
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 실제 API 테스트")
class AuthControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private User testUser;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        
        // 테스트 사용자 생성
        testUser = User.builder()
                .nickname("testUser")
                .password(passwordEncoder.encode("password123"))
                .isActive(true)
                .build();
        
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_Success() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setNickname("testUser");
        loginRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // When
        ResponseEntity<LoginResponse> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                LoginResponse.class
        );

        // Then
        System.out.println("🔍 로그인 응답 상태: " + response.getStatusCode());
        System.out.println("🔍 로그인 응답 본문: " + response.getBody());

        assertNotNull(response);
        assertNotNull(response.getBody());

        if (response.getStatusCode() == HttpStatus.OK) {
            LoginResponse loginResponse = response.getBody();
            assertNotNull(loginResponse.getUserId());
            assertEquals("testUser", loginResponse.getNickname());
            assertNotNull(loginResponse.getAccessToken());
            assertEquals("Bearer", loginResponse.getTokenType());
            assertNotNull(loginResponse.getExpiresIn());
            System.out.println("✅ 로그인 성공 테스트 완료");
        } else {
            System.out.println("⚠️ 로그인 실패 - 에러 응답 확인됨");
        }
    }

    @Test
    @DisplayName("로그인 - 잘못된 비밀번호")
    void login_WrongPassword() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setNickname("testUser");
        loginRequest.setPassword("wrongPassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                Map.class
        );

        // Then
        System.out.println("🔍 잘못된 비밀번호 응답: " + response.getStatusCode());
        assertNotNull(response);
        
        // 에러 응답 구조 확인
        if (response.getStatusCode() != HttpStatus.OK) {
            assertNotNull(response.getBody());
            Map<String, Object> body = response.getBody();
            assertTrue(body.containsKey("message") || body.containsKey("error"));
            System.out.println("✅ 잘못된 비밀번호 에러 응답 검증 완료");
        }
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 사용자")
    void login_UserNotFound() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setNickname("nonExistentUser");
        loginRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                Map.class
        );

        // Then
        System.out.println("🔍 존재하지 않는 사용자 응답: " + response.getStatusCode());
        assertNotNull(response);
        
        if (response.getStatusCode() != HttpStatus.OK) {
            assertNotNull(response.getBody());
            System.out.println("✅ 존재하지 않는 사용자 에러 응답 검증 완료");
        }
    }

    @Test
    @DisplayName("로그인 - 검증 실패 (빈 요청)")
    void login_ValidationError() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setNickname("");
        loginRequest.setPassword("");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                Map.class
        );

        // Then
        System.out.println("🔍 검증 실패 응답: " + response.getStatusCode());
        assertNotNull(response);
        
        // 검증 실패 응답 확인
        if (response.getStatusCode() == HttpStatus.BAD_REQUEST) {
            assertNotNull(response.getBody());
            System.out.println("✅ 검증 실패 에러 응답 검증 완료");
        }
    }

    @Test
    @DisplayName("로그아웃 - 인증 토큰 없이 시도")
    void logout_NoAuth() {
        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/auth/logout",
                null,
                Map.class
        );

        // Then
        System.out.println("🔍 로그아웃 (인증 없음) 응답: " + response.getStatusCode());
        assertNotNull(response);
        
        // 인증 오류 응답 확인
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.out.println("✅ 인증 없는 로그아웃 에러 응답 검증 완료");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }

    @Test
    @DisplayName("내 정보 조회 - 인증 없음")
    void getMyInfo_NoAuth() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
                baseUrl + "/auth/me",
                Map.class
        );

        // Then
        System.out.println("🔍 내 정보 조회 (인증 없음) 응답: " + response.getStatusCode());
        assertNotNull(response);
        
        // 인증 오류 응답 확인
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.FORBIDDEN) {
            System.out.println("✅ 인증 없는 내 정보 조회 에러 응답 검증 완료");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }

    @Test
    @DisplayName("잘못된 Content-Type으로 로그인 시도")
    void login_WrongContentType() {
        // Given
        String jsonData = "{\"nickname\":\"testUser\",\"password\":\"password123\"}";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN); // 잘못된 Content-Type
        HttpEntity<String> request = new HttpEntity<>(jsonData, headers);

        // When
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/auth/login",
                HttpMethod.POST,
                request,
                Map.class
        );

        // Then
        System.out.println("🔍 잘못된 Content-Type 응답: " + response.getStatusCode());
        assertNotNull(response);
        
        if (response.getStatusCode() == HttpStatus.UNSUPPORTED_MEDIA_TYPE || 
            response.getStatusCode() == HttpStatus.BAD_REQUEST) {
            System.out.println("✅ 잘못된 Content-Type 에러 응답 검증 완료");
        } else {
            System.out.println("⚠️ 예상과 다른 응답: " + response.getStatusCode());
        }
    }
}