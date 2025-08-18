package com.guineafigma.domain.system.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.security.jwt.JwtTokenProvider;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "System", description = "시스템 상태 모니터링 및 관리 API - 서비스 헬스체크, 에러 테스트, 개발 전용 도구")
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {
    private final DataSource dataSource;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Operation(
        summary = "서비스 헬스 체크", 
        description = "로고송 서비스와 데이터베이스 연결 상태를 확인합니다. " +
                    "서비스 전체의 건강 상태와 데이터베이스 연결 상태를 모니터링하는 데 사용됩니다."
    )
    @ApiErrorExamples({
            ErrorCode.INTERNAL_SERVER_ERROR,
    })
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        String dbStatus = "UNKNOWN";
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                dbStatus = "UP";
            } else {
                dbStatus = "DOWN";
            }
        } catch (SQLException e) {
            dbStatus = "DOWN";
        }
        healthInfo.put("status", "UP");
        healthInfo.put("db", dbStatus);
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("service", "logosong-server");

        return ApiResponse.success("서비스가 정상적으로 동작 중입니다.", healthInfo);
    }

    @Operation(
        summary = "에러 처리 테스트", 
        description = "전역 예외 처리기의 동작을 테스트하기 위해 의도적으로 RuntimeException을 발생시킵니다. " +
                    "개발 및 디버깅 목적으로 사용됩니다."
    )
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @GetMapping("/test-error")
    public ApiResponse<Void> testError() {
        throw new RuntimeException("테스트 에러입니다.");
    }

    @Operation(
        summary = "입력값 유효성 검증 테스트", 
        description = "API 요청 파라미터 유효성 검증 로직을 테스트합니다. " +
                    "빈 값이 입력되면 예외를 발생시켜 검증 로직을 확인할 수 있습니다."
    )
    @ApiErrorExamples({
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.REQUIRED_FIELD_MISSING,
            ErrorCode.INVALID_INPUT_VALUE
    })
    @GetMapping("/validation-test")
    public ApiResponse<String> validationTest(
            @Parameter(description = "테스트할 값", example = "test")
            @RequestParam String value
    ) {
        if (value.isEmpty()) {
            throw new RuntimeException("빈 값은 허용되지 않습니다.");
        }
        return ApiResponse.success("유효성 검증 통과", value);
    }

    // @Profile("dev") 추후 dev 환경에서만 사용하도록 수정
    @Operation(
        summary = "개발용 테스트 사용자 생성", 
        description = "개발 및 테스트 환경에서 사용할 더미 사용자 계정을 생성합니다. " +
                    "JWT 인증 기능을 테스트하거나 API 엔드포인트를 테스트할 때 필요합니다. " +
                    "(production 환경에서는 비활성화 예정)"
    )
    @PostMapping("/test-user")
    public ApiResponse<String> createTestUser(@RequestParam(defaultValue = "testuser") String nickname) {
        if (userRepository.findByNickname(nickname).isEmpty()) {
            User testUser = User.builder()
                    .nickname(nickname)
                    .password(passwordEncoder.encode("test1234"))
                    .isActive(true)
                    .build();
            userRepository.save(testUser);
            return ApiResponse.success("테스트 유저 생성됨: " + nickname + " (ID: " + testUser.getId() + ")");
        }
        User existingUser = userRepository.findByNickname(nickname).get();
        return ApiResponse.success("테스트 유저 이미 존재: " + nickname + " (ID: " + existingUser.getId() + ")");
    }

    // @Profile("dev") 추후 dev 환경에서만 사용하도록 수정
    @Operation(
        summary = "개발용 JWT 토큰 발급", 
        description = "지정된 사용자에 대해 개발 및 테스트용 JWT 액세스 토큰을 발급합니다. " +
                    "인증이 필요한 API 엔드포인트를 테스트할 때 사용합니다. " +
                    "(production 환경에서는 비활성화 예정)"
    )
    @PostMapping("/test-token")
    public ApiResponse<String> generateTestToken(@RequestParam(defaultValue = "testuser") String nickname) {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("테스트 유저를 먼저 생성해주세요."));
        
        String token = jwtTokenProvider.generateAccessToken(user.getId(), user.getNickname());
        return ApiResponse.success(token);
    }

    
}