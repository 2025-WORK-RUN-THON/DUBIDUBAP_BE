package com.guineafigma.domain.user.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.user.dto.request.LoginRequest;
import com.guineafigma.domain.user.dto.response.LoginResponse;
import com.guineafigma.domain.user.dto.response.UserResponse;
import com.guineafigma.domain.user.service.UserService;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.SwaggerConfig.ApiSuccessResponse;
import com.guineafigma.global.config.security.CustomUserPrincipal;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "사용자 인증 및 계정 관리 API - JWT 기반 로그인/가입 및 사용자 정보 관리")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "로그인/가입", 
        description = "닉네임과 비밀번호로 로그인하거나 신규 가입합니다. " +
                    "기존 사용자의 경우 로그인, 신규 사용자의 경우 자동 가입 후 로그인합니다. " +
                    "성공 시 JWT 액세스 토큰을 반환합니다."
    )
    @ApiSuccessResponse(
        message = "로그인/가입이 성공적으로 처리되었습니다.", 
        dataType = LoginResponse.class
    )
    @ApiErrorExamples({
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.INVALID_PASSWORD,
            ErrorCode.USER_NOT_ACTIVE,
            ErrorCode.NICKNAME_TOO_SHORT,
            ErrorCode.NICKNAME_TOO_LONG,
            ErrorCode.PASSWORD_TOO_SHORT
    })
    public ApiResponse<LoginResponse> login(
        @Parameter(description = "로그인 요청 정보 - 닉네임과 비밀번호 포함", required = true)
        @Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.authenticateUser(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "JWT")
    @Operation(
        summary = "로그아웃", 
        description = "현재 로그인된 사용자를 로그아웃 처리하고 세션을 종료합니다. " +
                    "JWT 토큰을 사용해 인증된 사용자만 로그아웃 가능합니다."
    )
    @ApiSuccessResponse(message = "로그아웃이 성공적으로 처리되었습니다.")
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.USER_NOT_FOUND
    })
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        userService.logoutUser(userPrincipal.getId());
        return ApiResponse.success();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "JWT")
    @Operation(
        summary = "내 정보 조회", 
        description = "현재 로그인된 사용자의 상세 정보를 조회합니다. " +
                    "JWT 토큰을 통해 인증된 사용자의 닉네임, ID, 계정 상태 등을 반환합니다."
    )
    @ApiSuccessResponse(
        message = "사용자 정보 조회가 성공적으로 처리되었습니다.", 
        dataType = UserResponse.class
    )
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.USER_NOT_ACTIVE
    })
    public ApiResponse<UserResponse> getMyInfo(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        UserResponse response = userService.getUserById(userPrincipal.getId());
        return ApiResponse.success(response);
    }
}