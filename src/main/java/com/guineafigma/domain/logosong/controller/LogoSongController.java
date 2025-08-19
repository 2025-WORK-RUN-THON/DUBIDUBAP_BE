package com.guineafigma.domain.logosong.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.common.response.PagedResponse;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationStatusResponse;
import com.guineafigma.domain.logosong.service.LogoSongService;
import com.guineafigma.domain.logosong.service.IntegratedLogoSongService;
import com.guineafigma.domain.logosong.service.MusicGenerationPollingService;
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
import org.springdoc.core.annotations.ParameterObject;

import java.util.Map;

@RestController
@RequestMapping("/logosongs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LogoSong", description = "로고송 관리 API")
public class LogoSongController {

    private final LogoSongService logoSongService;
    private final IntegratedLogoSongService integratedLogoSongService;
    private final MusicGenerationPollingService pollingService;

    

    @GetMapping("/{id}")
    @Operation(summary = "로고송 조회", description = "특정 로고송을 조회하고 조회수를 증가시킵니다.")
    @ApiSuccessResponse(message = "로고송 조회가 성공적으로 처리되었습니다.", dataType = LogoSongResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND
    })
    public ApiResponse<LogoSongResponse> getLogoSong(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LogoSongResponse response = logoSongService.incrementViewCountWithLike(
                id,
                userPrincipal != null ? userPrincipal.getId() : null
        );
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(
        summary = "전시회 목록 조회",
        description = "전시회(공개) 로고송 목록을 페이지네이션으로 조회합니다.\n\n" +
                "설명:\n" +
                "- 전시회 목록은 공개(`isPublic=true`)로 설정된 로고송만 노출됩니다.\n" +
                "- 비공개(`isPublic=false`) 로고송은 소유자 외에는 목록과 단건 모두 노출되지 않습니다.\n" +
                "- 로그인한 경우, 각 항목의 좋아요 여부(`isLiked`)가 반영됩니다.\n\n" +
                "쿼리 파라미터:\n" +
                "- page: 0부터 시작하는 페이지 번호 (예: 0)\n" +
                "- size: 페이지 당 항목 수 (예: 10)\n" +
                "- sort: 정렬 기준, 쉼표로 방향 지정 (예: createdAt,desc)"
    )
    @ApiSuccessResponse(
            message = "전시회(공개) 목록 조회가 성공적으로 처리되었습니다.",
            dataType = PagedResponse.class,
            isArray = true,
            dataExample = "{\n  'content': [\n    { 'id': 1, 'serviceName': '카페 뒤비뒤밥', 'likeCount': 12, 'isLiked': true },\n    { 'id': 2, 'serviceName': '분식 더밥', 'likeCount': 5, 'isLiked': null }\n  ],\n  'pagination': { 'limit': 10, 'currentPage': 1, 'totalPage': 5 }\n}"
    )
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE
    })
    public ApiResponse<PagedResponse<LogoSongResponse>> getAllLogoSongs(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(value = "page", required = false) Integer pageParam,
            @RequestParam(value = "size", required = false) Integer sizeParam,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        // 쿼리 파라미터 원본 값 기준 검증: page>=0, size>0
        if ((pageParam != null && pageParam < 0) || (sizeParam != null && sizeParam <= 0)) {
            throw new com.guineafigma.global.exception.BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PagedResponse<LogoSongResponse> response;
        if (userPrincipal != null) {
            response = logoSongService.getAllLogoSongs(pageable, userPrincipal.getId());
        } else {
            response = logoSongService.getAllLogoSongs(pageable);
        }
        return ApiResponse.success(response);
    }

    @GetMapping("/popular")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "인기 로고송 조회", description = "좋아요 수가 많은 순으로 로고송을 조회합니다.")
    @ApiSuccessResponse(message = "인기 로고송 목록 조회가 성공적으로 처리되었습니다.", dataType = PagedResponse.class, isArray = true)
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE
    })
    public ApiResponse<PagedResponse<LogoSongResponse>> getPopularLogoSongs(
            @ParameterObject
            @PageableDefault(size = 10, sort = "likeCount", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(value = "page", required = false) Integer pageParam,
            @RequestParam(value = "size", required = false) Integer sizeParam,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        if ((pageParam != null && pageParam < 0) || (sizeParam != null && sizeParam <= 0)) {
            throw new com.guineafigma.global.exception.BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PagedResponse<LogoSongResponse> response;
        if (userPrincipal != null) {
            response = logoSongService.getPopularLogoSongs(pageable, userPrincipal.getId());
        } else {
            response = logoSongService.getPopularLogoSongs(pageable);
        }
        return ApiResponse.success(response);
    }

    @PostMapping("/guides")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "JWT")
    @Operation(
        summary = "가사 생성", 
        description = "브랜드 정보를 기반으로 LogoSong 엔티티를 생성한 뒤 OpenAI API로 '가사만' 생성하여 저장하고, 업데이트된 전체 레코드를 반환합니다. 비디오 가이드라인은 생성하지 않습니다."
    )
    @ApiSuccessResponse(
        message = "가사 생성이 성공적으로 처리되었습니다.", 
        dataType = LogoSongResponse.class,
        httpStatus = 201
    )
    @ApiErrorExamples({
        ErrorCode.VALIDATION_ERROR,
        ErrorCode.SERVICE_NAME_REQUIRED,
        ErrorCode.LYRICS_GENERATION_FAILED,
        ErrorCode.INTERNAL_SERVER_ERROR
    })
    public ApiResponse<LogoSongResponse> generateGuides(
        @Parameter(description = "로고송 생성 요청 정보 - 브랜드 및 음악 스타일 정보 포함", required = true)
        @Valid @RequestBody LogoSongCreateRequest request,
        @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        if (userId == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        LogoSongResponse response = integratedLogoSongService.createLogoSongWithGuidesOnly(request, userId);
        return ApiResponse.successCreated(response);
    }

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

    @PostMapping("/{id}/visibility")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "로고송 공개 여부 변경", description = "본인 로고송의 공개 여부를 변경합니다. 기본값은 비공개(false)입니다.")
    @ApiSuccessResponse(message = "공개 여부가 성공적으로 변경되었습니다.")
    public ApiResponse<Void> updateVisibility(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @Parameter(description = "공개 여부", required = true) @RequestParam("public") boolean publicVisible,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        // 사용자 소유 검증 로직은 추후 userId가 모델에 포함될 때 강화 가능
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        if (userId == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        logoSongService.updateVisibility(id, publicVisible, userId);
        return ApiResponse.success();
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
            @ParameterObject
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
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        if (userPrincipal == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        PagedResponse<LogoSongResponse> response = logoSongService.getLikedLogoSongs(userPrincipal.getId(), pageable);
        return ApiResponse.success(response);
    }

    // =========================== 새로운 Suno API 통합 엔드포인트들 ===========================

    @PostMapping("/with-generation")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "로고송 생성 (가사+음악 통합)", description = "가사/비디오 가이드라인 생성 후 음악 생성을 비동기로 시작합니다.")
    @ApiSuccessResponse(message = "로고송 생성이 시작되었습니다.", dataType = LogoSongResponse.class, httpStatus = 201)
    @ApiErrorExamples({
            ErrorCode.VALIDATION_ERROR,
            ErrorCode.SERVICE_NAME_REQUIRED,
            ErrorCode.INVALID_MUSIC_GENRE,
            ErrorCode.INVALID_VERSION_TYPE,
            ErrorCode.LYRICS_GENERATION_FAILED
    })
    public ApiResponse<LogoSongResponse> createLogoSongWithGeneration(
            @Valid @RequestBody LogoSongCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        Long userId = userPrincipal != null ? userPrincipal.getId() : null;
        if (userId == null) {
            return ApiResponse.error(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        LogoSongResponse response = integratedLogoSongService.createLogoSongWithGeneration(request, userId);
        return ApiResponse.successCreated(response);
    }

    @PostMapping("/{id}/generate-music")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "음악 생성 트리거", 
           description = "기존 로고송에 대해 음악 생성을 시작합니다.\n\n" +
                   "처리 과정:\n" +
                   "- Suno API를 호출하여 음악 생성 요청\n" +
                   "- 비동기로 음악 생성 진행\n" +
                   "- 완료 시 음악 URL과 이미지 URL 자동 업데이트\n" +
                   "- 가사가 없는 경우 생성 불가\n\n" +
                   "상태 확인: `/api/v1/logosongs/{id}/status`")
    @ApiSuccessResponse(message = "음악 생성이 시작되었습니다.")
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.MUSIC_GENERATION_IN_PROGRESS,
            ErrorCode.LYRICS_GENERATION_FAILED
    })
    public ApiResponse<Void> generateMusic(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        integratedLogoSongService.triggerMusicGeneration(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "음악 생성 상태 조회", description = "단일 엔드포인트로 음악 생성 상태를 조회합니다. 내부 캐시/DB 확인 후 필요시 외부 상태도 반영합니다.")
    @ApiSuccessResponse(message = "음악 생성 상태 조회가 성공적으로 처리되었습니다.", dataType = MusicGenerationStatusResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND
    })
    public ApiResponse<MusicGenerationStatusResponse> getStatus(
            @Parameter(description = "로고송 ID") @PathVariable Long id) {
        MusicGenerationStatusResponse status = pollingService.getPollingStatus(id);
        return ApiResponse.success(status);
    }

    

    @PostMapping("/{id}/regenerate-lyrics")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "가사 재생성", 
               description = "기존 로고송의 가사만 재생성합니다.\n\n" +
                       "처리 과정:\n" +
                       "- OpenAI API를 호출하여 새로운 가사 생성\n" +
                       "- 기존 가사를 새로운 가사로 교체\n" +
                       "- 음악 상태를 PENDING으로 변경 (재생성 필요)\n\n" +
                       "주의사항:\n" +
                       "- 로고송 ID가 존재해야 함\n" +
                       "- OpenAI API 키가 설정되어야 함")
    @ApiSuccessResponse(message = "가사 재생성이 성공적으로 처리되었습니다.", dataType = LogoSongResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.LYRICS_GENERATION_FAILED
    })
    public ApiResponse<LogoSongResponse> regenerateLyrics(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @Valid @RequestBody LogoSongCreateRequest request,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LogoSongResponse updated = integratedLogoSongService.regenerateLyricsOnly(id, request);
        return ApiResponse.success(updated);
    }

    @PostMapping("/{id}/regenerate-video-guide")
    @SecurityRequirement(name = "JWT")
    @Operation(summary = "비디오 가이드라인 (재)생성",
               description = "기존 로고송 ID 기반으로 비디오 가이드라인만 생성/재생성합니다. 사용자 입력은 이미 DB에 저장되어 있으므로 추가 요청 본문이 필요하지 않습니다.")
    @ApiSuccessResponse(message = "비디오 가이드라인 (재)생성이 성공적으로 처리되었습니다.", dataType = LogoSongResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.LYRICS_GENERATION_FAILED
    })
    public ApiResponse<LogoSongResponse> regenerateVideoGuide(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        LogoSongResponse updated = integratedLogoSongService.regenerateVideoGuideOnly(id);
        return ApiResponse.success(updated);
    }

    // 상태 엔드포인트 통합: /{id}/status

    // =========================== Suno API 콜백 엔드포인트 ===========================

    // Suno 콜백은 별도 SunoCallbackController에서 처리
}