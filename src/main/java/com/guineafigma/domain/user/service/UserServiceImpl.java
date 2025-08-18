package com.guineafigma.domain.user.service;

import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.dto.response.UserResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.global.config.security.jwt.JwtTokenProvider;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public LoginResponse authenticateUser(LoginRequest request) {
        String nickname = request.getNickname();
        String password = request.getPassword();
        
        Optional<User> existingUser = userRepository.findByNickname(nickname);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }

            // 과거 로그아웃 버그로 비활성화된 사용자는 재로그인 시 자동 활성화
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                user.activate();
                userRepository.save(user);
                log.info("비활성 사용자 자동 활성화: {}", nickname);
            }

            String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getNickname());
            log.info("사용자 로그인 성공: {}", nickname);

            return LoginResponse.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                    .isNewUser(false)
                    .message("로그인 성공")
                    .build();
        } else {
            User newUser = User.builder()
                    .nickname(nickname)
                    .password(passwordEncoder.encode(password))
                    .isActive(true)
                    .build();
            
            User savedUser = userRepository.save(newUser);
            String token = jwtTokenProvider.generateAccessToken(savedUser.getId(), savedUser.getNickname());
            log.info("신규 사용자 가입 및 로그인 성공: {}", nickname);
            
            return LoginResponse.builder()
                    .userId(savedUser.getId())
                    .nickname(savedUser.getNickname())
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                    .isNewUser(true)
                    .message("가입 성공과 함께 로그인되었습니다")
                    .build();
        }
    }

    @Override
    @Transactional
    public void logoutUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (!user.getIsActive()) {
            log.info("이미 비활성 사용자 로그아웃 요청: {}", user.getNickname());
            return;
        }
        
        // 로그아웃 시 계정을 비활성화하여 기존 토큰 무효화
        user.deactivate();
        userRepository.save(user);
        log.info("사용자 로그아웃: {} (계정 비활성화)", user.getNickname());
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (!user.getIsActive()) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }
        
        return UserResponse.from(user);
    }

    @Override
    public User findActiveUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        if (!user.getIsActive()) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }
        
        return user;
    }
}