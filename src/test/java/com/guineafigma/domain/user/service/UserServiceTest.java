package com.guineafigma.domain.user.service;

import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import com.guineafigma.global.config.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .nickname("testUser")
                .password("encodedPassword123")
                .isActive(true)
                .build();
        
        // Mock repository에서 반환할 때 ID 설정
        testUser.setId(1L);

        loginRequest = new LoginRequest();
        loginRequest.setNickname("testUser");
        loginRequest.setPassword("password123");
    }

    @Test
    @DisplayName("로그인 성공 - 기존 사용자")
    void authenticateUser_Success_ExistingUser() {
        // given
        when(userRepository.findByNickname("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(1L, "testUser")).thenReturn("test.jwt.token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);

        // when
        LoginResponse response = userService.authenticateUser(loginRequest);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testUser", response.getNickname());
        assertEquals("test.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertFalse(response.getIsNewUser());
        assertEquals("로그인 성공", response.getMessage());

        verify(userRepository).findByNickname("testUser");
        verify(passwordEncoder).matches("password123", "encodedPassword123");
        verify(jwtTokenProvider).generateAccessToken(1L, "testUser");
    }

    @Test
    @DisplayName("로그인 성공 - 새 사용자 생성")
    void authenticateUser_Success_NewUser() {
        // given
        when(userRepository.findByNickname("testUser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123");
        when(jwtTokenProvider.generateAccessToken(any(), anyString())).thenReturn("test.jwt.token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);

        User newUser = User.builder()
                .nickname("testUser")
                .password("encodedPassword123")
                .isActive(true)
                .build();
        newUser.setId(2L); // ID 설정
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // when
        LoginResponse response = userService.authenticateUser(loginRequest);

        // then
        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        assertEquals("testUser", response.getNickname());
        assertTrue(response.getIsNewUser());
        assertEquals("가입 성공과 함께 로그인되었습니다", response.getMessage());

        verify(userRepository).findByNickname("testUser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void authenticateUser_Failure_InvalidPassword() {
        // given
        when(userRepository.findByNickname("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(false);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.authenticateUser(loginRequest));
        
        assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());

        verify(userRepository).findByNickname("testUser");
        verify(passwordEncoder).matches("password123", "encodedPassword123");
        verify(jwtTokenProvider, never()).generateAccessToken(any(), anyString());
    }

    @Test
    @DisplayName("로그인 성공 - 비활성 사용자 자동 활성화")
    void authenticateUser_Success_InactiveUserAutoActivation() {
        // given
        User inactiveUser = User.builder()
                .nickname("testUser")
                .password("encodedPassword123")
                .isActive(false)
                .build();
        inactiveUser.setId(1L); // ID 설정

        when(userRepository.findByNickname("testUser")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("password123", "encodedPassword123")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(1L, "testUser")).thenReturn("test.jwt.token");
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3600L);

        User activatedUser = User.builder()
                .nickname("testUser")
                .password("encodedPassword123")
                .isActive(true)
                .build();
        activatedUser.setId(1L); // ID 설정
        when(userRepository.save(any(User.class))).thenReturn(activatedUser);

        // when
        LoginResponse response = userService.authenticateUser(loginRequest);

        // then
        assertNotNull(response);
        assertFalse(response.getIsNewUser());
        assertEquals("로그인 성공", response.getMessage());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 ID로 조회 성공")
    void getUserById_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when
        var foundUser = userService.getUserById(1L);

        // then
        assertNotNull(foundUser);
        assertEquals(1L, foundUser.getId());
        assertEquals("testUser", foundUser.getNickname());

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("사용자 ID로 조회 실패 - 존재하지 않는 사용자")
    void getUserById_Failure_UserNotFound() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.getUserById(999L));
        
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

        verify(userRepository).findById(999L);
    }

    @Test
    @DisplayName("활성 사용자 ID로 조회 성공")
    void findActiveUserById_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when
        User foundUser = userService.findActiveUserById(1L);

        // then
        assertNotNull(foundUser);
        assertTrue(foundUser.getIsActive());

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("활성 사용자 ID로 조회 실패 - 비활성 사용자")
    void findActiveUserById_Failure_InactiveUser() {
        // given
        User inactiveUser = User.builder()
                .nickname("testUser")
                .password("encodedPassword123")
                .isActive(false)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(inactiveUser));

        // when & then
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.findActiveUserById(1L));

        assertEquals(ErrorCode.USER_NOT_ACTIVE, exception.getErrorCode());

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logoutUser_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        userService.logoutUser(1L);

        // then
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
        assertFalse(testUser.getIsActive());
    }
}
