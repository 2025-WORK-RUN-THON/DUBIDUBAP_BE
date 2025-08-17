package com.guineafigma.domain.system.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.enums.Role;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.common.enums.SocialType;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "System", description = "시스템 관리 및 모니터링 API")
@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {
    private final DataSource dataSource;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository memberRepository;

    @Operation(summary = "헬스 체크", description = "서비스 및 DB 상태를 확인합니다.")
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
        healthInfo.put("service", "guineafigma-server");

        return ApiResponse.success("서비스가 정상적으로 동작 중입니다.", healthInfo);
    }

    @Operation(summary = "에러 테스트", description = "에러 처리를 테스트합니다.")
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.INTERNAL_SERVER_ERROR
    })
    @GetMapping("/test-error")
    public ApiResponse<Void> testError() {
        throw new RuntimeException("테스트 에러입니다.");
    }

    @Operation(summary = "유효성 검증 테스트", description = "입력값 유효성 검증을 테스트합니다.")
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
    @Operation(summary = "테스트 유저 생성", description = "개발 환경용 테스트 유저를 생성합니다. JWT 인증 테스트를 위해 필요합니다.")
    @PostMapping("/test-user")
    public ApiResponse<String> createTestUser(@RequestParam(defaultValue = "test@guineafigma.com") String email) {
        if (memberRepository.findByEmail(email).isEmpty()) {
            User testUser = User.builder()
                    .email(email)
                    .socialType(SocialType.GOOGLE)
                    .role(Role.USER)
                    .build();
            memberRepository.save(testUser);
            return ApiResponse.success("테스트 유저 생성됨: " + email + " (ID: " + testUser.getId() + ")");
        }
        User existingUser = memberRepository.findByEmail(email).get();
        return ApiResponse.success("테스트 유저 이미 존재: " + email + " (ID: " + existingUser.getId() + ")");
    }

    // @Profile("dev") 추후 dev 환경에서만 사용하도록 수정
    @Operation(summary = "테스트 JWT 토큰 발급", description = "개발 환경용 JWT 토큰을 발급합니다.")
    @PostMapping("/test-token")
    public ApiResponse<String> generateTestToken(@RequestParam(defaultValue = "test@guineafigma.com") String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            email, 
            null, 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        String token = jwtTokenProvider.generateToken(auth);
        return ApiResponse.success(token);
    }

    
}