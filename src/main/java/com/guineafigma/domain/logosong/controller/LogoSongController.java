package com.guineafigma.domain.logosong.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.common.response.PagedResponse;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.service.LogoSongService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logosongs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LogoSong", description = "로고송 관리 API")
public class LogoSongController {

    private final LogoSongService logoSongService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "로고송 생성", description = "새로운 로고송을 생성합니다.")
    @ApiSuccessResponse(message = "로고송이 성공적으로 생성되었습니다.", dataType = LogoSongResponse.class)
    @ApiErrorExamples({
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.SERVICE_NAME_REQUIRED,
            ErrorCode.INVALID_MUSIC_GENRE,
            ErrorCode.INVALID_VERSION_TYPE
    })
    public ApiResponse<LogoSongResponse> createLogoSong(
            @Valid @RequestBody LogoSongCreateRequest request) {
        LogoSongResponse response = logoSongService.createLogoSong(request);
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "로고송 조회", description = "특정 로고송을 조회하고 조회수를 증가시킵니다.")
    @ApiSuccessResponse(message = "로고송 조회가 성공적으로 처리되었습니다.", dataType = LogoSongResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND
    })
    public ApiResponse<LogoSongResponse> getLogoSong(
            @Parameter(description = "로고송 ID") @PathVariable Long id) {
        LogoSongResponse response = logoSongService.incrementViewCount(id);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(summary = "로고송 목록 조회", description = "로고송 목록을 페이지네이션으로 조회합니다.")
    @ApiSuccessResponse(message = "로고송 목록 조회가 성공적으로 처리되었습니다.", dataType = PagedResponse.class, isArray = true)
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE
    })
    public ApiResponse<PagedResponse<LogoSongResponse>> getAllLogoSongs(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        PagedResponse<LogoSongResponse> response = logoSongService.getAllLogoSongs(pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/popular")
    @Operation(summary = "인기 로고송 조회", description = "좋아요 수가 많은 순으로 로고송을 조회합니다.")
    @ApiSuccessResponse(message = "인기 로고송 목록 조회가 성공적으로 처리되었습니다.", dataType = PagedResponse.class, isArray = true)
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE
    })
    public ApiResponse<PagedResponse<LogoSongResponse>> getPopularLogoSongs(
            @PageableDefault(size = 10, sort = "likeCount", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        PagedResponse<LogoSongResponse> response = logoSongService.getPopularLogoSongs(pageable);
        return ApiResponse.success(response);
    }

    // guides 생성 엔드포인트는 assistant 도메인으로 이동됨 (/api/v1/assistant/guides)

    @PostMapping("/{id}/like")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "로고송 좋아요 토글", description = "로고송에 좋아요를 추가하거나 제거합니다.")
    @ApiSuccessResponse(message = "좋아요 상태가 성공적으로 변경되었습니다.")
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.USER_NOT_FOUND
    })
    public ApiResponse<Void> toggleLike(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        logoSongService.toggleLike(id, userPrincipal.getId());
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/like-status")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "좋아요 상태 확인", description = "사용자의 특정 로고송 좋아요 상태를 확인합니다.")
    @ApiSuccessResponse(message = "좋아요 상태 조회가 성공적으로 처리되었습니다.", dataType = Boolean.class)
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.USER_NOT_FOUND
    })
    public ApiResponse<Boolean> getLikeStatus(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        boolean isLiked = logoSongService.isLikedByUser(id, userPrincipal.getId());
        return ApiResponse.success(isLiked);
    }

    @GetMapping("/my")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "내 로고송 목록 조회", description = "현재 로그인한 사용자가 생성한 로고송 목록을 조회합니다.")
    @ApiSuccessResponse(message = "내 로고송 목록 조회가 성공적으로 처리되었습니다.", dataType = PagedResponse.class, isArray = true)
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.USER_NOT_FOUND
    })
    public ApiResponse<PagedResponse<LogoSongResponse>> getMyLogoSongs(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        if (userPrincipal == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        PagedResponse<LogoSongResponse> response = logoSongService.getMyLogoSongs(userPrincipal.getId(), pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/my/liked")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "좋아요한 로고송 목록 조회", description = "현재 로그인한 사용자가 좋아요한 로고송 목록을 조회합니다.")
    @ApiSuccessResponse(message = "좋아요한 로고송 목록 조회가 성공적으로 처리되었습니다.", dataType = PagedResponse.class, isArray = true)
    @ApiErrorExamples({
            ErrorCode.AUTHENTICATION_REQUIRED,
            ErrorCode.INVALID_TOKEN,
            ErrorCode.USER_NOT_FOUND
    })
    public ApiResponse<PagedResponse<LogoSongResponse>> getLikedLogoSongs(
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        if (userPrincipal == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        PagedResponse<LogoSongResponse> response = logoSongService.getLikedLogoSongs(userPrincipal.getId(), pageable);
        return ApiResponse.success(response);
    }
}