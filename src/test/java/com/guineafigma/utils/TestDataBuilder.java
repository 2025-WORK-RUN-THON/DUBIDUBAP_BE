package com.guineafigma.utils;

import com.guineafigma.common.enums.MusicGenerationStatus;
import com.guineafigma.common.enums.VersionType;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.entity.LogoSong;
import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.dto.response.UserResponse;
import com.guineafigma.domain.user.entity.User;

public class TestDataBuilder {

    // User 관련 테스트 데이터
    public static User createTestUser() {
        return User.builder()
                .nickname("testUser")
                .password("encodedPassword")
                .isActive(true)
                .build();
    }

    public static LoginRequest createValidLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setNickname("testUser");
        request.setPassword("password123");
        return request;
    }

    public static LoginRequest createInvalidLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setNickname("t"); // 너무 짧음
        request.setPassword("123"); // 너무 짧음
        return request;
    }

    public static LoginResponse createLoginResponse() {
        return LoginResponse.builder()
                .userId(1L)
                .nickname("testUser")
                .accessToken("test-jwt-token")
                .tokenType("Bearer")
                .expiresIn(14400L)
                .isNewUser(false)
                .message("로그인 성공")
                .build();
    }

    public static UserResponse createUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .nickname("testUser")
                .isActive(true)
                .build();
    }

    // LogoSong 관련 테스트 데이터
    public static LogoSongCreateRequest createValidLogoSongRequest() {
        return LogoSongCreateRequest.builder()
                .serviceName("테스트 서비스")
                .slogan("혁신적인 서비스")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre("ELECTRONIC")
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송입니다.")
                .build();
    }

    public static LogoSongCreateRequest createInvalidLogoSongRequest() {
        return LogoSongCreateRequest.builder()
                .serviceName("") // 빈 값
                .musicGenre("INVALID_GENRE")
                .build();
    }

    public static LogoSong createTestLogoSong() {
        LogoSong logoSong = LogoSong.builder()
                .serviceName("테스트 서비스")
                .slogan("테스트 슬로건")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre("ELECTRONIC")
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송")
                .lyrics("테스트 가사 내용")
                .videoGuideline("테스트 비디오 가이드라인")
                .musicStatus(MusicGenerationStatus.PENDING)
                .likeCount(0)
                .viewCount(0)
                .build();
        
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = LogoSong.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(logoSong, 1L);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        return logoSong;
    }

    public static LogoSong createLogoSongWithMusic() {
        LogoSong logoSong = LogoSong.builder()
                .serviceName("테스트 서비스")
                .slogan("테스트 슬로건")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre("ELECTRONIC")
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송")
                .lyrics("테스트 가사 내용")
                .videoGuideline("테스트 비디오 가이드라인")
                .musicStatus(MusicGenerationStatus.COMPLETED)
                .generatedMusicUrl("https://example.com/audio.mp3")
                .imageUrl("https://example.com/image.jpg")
                .likeCount(5)
                .viewCount(10)
                .build();
        
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = LogoSong.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(logoSong, 2L);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        return logoSong;
    }

    public static LogoSongResponse createLogoSongResponse() {
        LogoSong logoSong = createTestLogoSong();
        return LogoSongResponse.from(logoSong, false); // isLiked = false
    }

    public static LogoSongResponse createLogoSongResponseWithLike() {
        LogoSong logoSong = createTestLogoSong();
        return LogoSongResponse.from(logoSong, true); // isLiked = true
    }

    // 진행 상태별 로고송 데이터
    public static LogoSong createLogoSongInProgress() {
        LogoSong logoSong = LogoSong.builder()
                .serviceName("테스트 서비스")
                .slogan("테스트 슬로건")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre("ELECTRONIC")
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송")
                .lyrics("테스트 가사 내용")
                .videoGuideline("테스트 비디오 가이드라인")
                .musicStatus(MusicGenerationStatus.PROCESSING)
                .sunoTaskId("task_123456")
                .likeCount(0)
                .viewCount(0)
                .build();
        
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = LogoSong.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(logoSong, 3L);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        return logoSong;
    }

    public static LogoSong createLogoSongFailed() {
        LogoSong logoSong = LogoSong.builder()
                .serviceName("테스트 서비스")
                .slogan("테스트 슬로건")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre("ELECTRONIC")
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송")
                .lyrics("테스트 가사 내용")
                .videoGuideline("테스트 비디오 가이드라인")
                .musicStatus(MusicGenerationStatus.FAILED)
                // errorMessage 필드 없음 - 엔티티에는 없는 필드
                .likeCount(0)
                .viewCount(0)
                .build();
        
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = LogoSong.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(logoSong, 4L);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        return logoSong;
    }

    // 페이지네이션용 테스트 데이터
    public static LogoSong createLogoSongWithId(Long id) {
        LogoSong logoSong = LogoSong.builder()
                .serviceName("테스트 서비스 " + id)
                .slogan("테스트 슬로건")
                .industry("IT")
                .marketingItem("혁신")
                .targetCustomer("개발자")
                .moodTone("밝고 활기찬")
                .musicGenre("ELECTRONIC")
                .version(VersionType.SHORT)
                .additionalInfo("테스트용 로고송")
                .lyrics("테스트 가사 내용")
                .videoGuideline("테스트 비디오 가이드라인")
                .musicStatus(MusicGenerationStatus.PENDING)
                .likeCount(0)
                .viewCount(0)
                .build();
        
        // Reflection을 통해 ID 설정
        try {
            java.lang.reflect.Field idField = LogoSong.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(logoSong, id);
        } catch (Exception e) {
            // 테스트용으로 ID 설정 실패해도 진행
        }
        
        return logoSong;
    }

    // Suno API 콜백 데이터
    public static String createSunoCallbackJson() {
        return """
                {
                    "taskId": "task_123456",
                    "status": "completed",
                    "audioUrl": "https://suno.example.com/audio.mp3",
                    "imageUrl": "https://suno.example.com/image.jpg"
                }
                """;
    }

    public static String createInvalidSunoCallbackJson() {
        return """
                {
                    "status": "completed"
                }
                """;
    }
}