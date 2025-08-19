package com.guineafigma.controller;

import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.logosong.repository.LogoSongLikeRepository;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:logosong-my-endpoints;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.cloud.aws.stack.auto=false",
                "spring.cloud.aws.region.auto=false",
                "jwt.secret-key=test-jwt-secret-key-for-my-endpoints"
        })
@ActiveProfiles("dev")
@Import(com.guineafigma.config.TestConfig.class)
@DisplayName("'my' 및 'my/liked' 응답 필드(userId,userNickname) 검증 테스트")
class LogoSongMyEndpointsUserFieldsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LogoSongRepository logoSongRepository;

    @Autowired
    private LogoSongLikeRepository logoSongLikeRepository;

    private String baseUrl;
    private User testUser;
    private String accessToken;
    private Long testUserId;
    private String testUserNickname;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // 로그인하여 JWT 획득
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setNickname("myUser");
        loginRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<Map<String, Object>> loginResp = restTemplate.exchange(
                baseUrl + "/api/v1/auth/login",
                HttpMethod.POST,
                request,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );
        System.out.println("login status=" + loginResp.getStatusCode());
        System.out.println("login body=" + loginResp.getBody());
        assertNotNull(loginResp.getBody(), "login body is null");
        Map<String, Object> loginBody = loginResp.getBody();
        assertNotNull(loginBody);
        Object dataObj = loginBody.get("data");
        assertNotNull(dataObj, "login data is null");
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) dataObj;
        accessToken = (String) dataMap.get("accessToken");
        assertNotNull(accessToken, "accessToken is null");

        // 로그인 성공 데이터에서 사용자 정보 취득 후 DB에서 조회
        Number uidNum = (Number) dataMap.get("userId");
        assertNotNull(uidNum, "userId is null");
        testUserId = uidNum.longValue();
        testUserNickname = (String) dataMap.get("nickname");
        assertNotNull(testUserNickname, "nickname is null");

        testUser = userRepository.findById(testUserId).orElseThrow();
    }

    @Test
    @DisplayName("내 로고송 목록 조회 시 각 항목에 userId/userNickname 포함")
    void myList_ShouldContainUserFields() {
        // 내 소유 로고송 한 개 저장
        LogoSong mySong = LogoSong.builder()
                .user(testUser)
                .serviceName("내 로고송")
                .musicGenre("POP")
                .version(com.guineafigma.common.enums.VersionType.SHORT)
                .build();
        logoSongRepository.saveAndFlush(mySong);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/my?page=0&size=10",
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = Objects.requireNonNull(resp.getBody());
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        assertFalse(content.isEmpty());
        Map<String, Object> first = content.get(0);
        assertEquals(testUserId.intValue(), ((Number) first.get("userId")).intValue());
        assertEquals(testUserNickname, first.get("userNickname"));
    }

    @Test
    @DisplayName("좋아요한 로고송 목록 조회 시 각 항목에 userId/userNickname 포함")
    void likedList_ShouldContainUserFields() {
        // 다른 사용자의 로고송을 하나 생성하고, 내가 좋아요 추가
        User other = userRepository.save(User.builder()
                .nickname("otherUser")
                .password(passwordEncoder.encode("password456"))
                .isActive(true)
                .build());

        LogoSong otherSong = LogoSong.builder()
                .user(other)
                .serviceName("다른 유저 곡")
                .musicGenre("POP")
                .version(com.guineafigma.common.enums.VersionType.SHORT)
                .build();
        otherSong = logoSongRepository.saveAndFlush(otherSong);

        // 좋아요 토글 호출 (인증 필요)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> likeResp = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/" + otherSong.getId() + "/like",
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        assertEquals(HttpStatus.OK, likeResp.getStatusCode());

        // 좋아요 목록 조회
        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                baseUrl + "/api/v1/logosongs/my/liked?page=0&size=10",
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = Objects.requireNonNull(resp.getBody());
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        assertFalse(content.isEmpty());

        // 좋아요 목록 항목 각각에 사용자 필드 포함 확인
        for (Map<String, Object> item : content) {
            assertNotNull(item.get("userId"));
            assertNotNull(item.get("userNickname"));
        }
    }
}


