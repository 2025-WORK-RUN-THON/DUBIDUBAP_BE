package com.guineafigma.domain.logosong.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.common.response.PagedResponse;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
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
        LogoSongResponse response;
        if (userPrincipal != null) {
            response = logoSongService.getLogoSongWithLike(id, userPrincipal.getId());
        } else {
            response = logoSongService.incrementViewCount(id);
        }
        return ApiResponse.success(response);
    }

    @GetMapping
    @Operation(
        summary = "로고송 목록 조회",
        description = "로고송 목록을 페이지네이션으로 조회합니다.\n\n" +
                "쿼리 파라미터:\n" +
                "- page: 0부터 시작하는 페이지 번호 (예: 0)\n" +
                "- size: 페이지 당 항목 수 (예: 10)\n" +
                "- sort: 정렬 기준, 쉼표로 방향 지정 (예: createdAt,desc)"
    )
    @ApiSuccessResponse(
            message = "로고송 목록 조회가 성공적으로 처리되었습니다.",
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
    @Operation(
        summary = "가사/비디오 가이드라인 생성", 
        description = "브랜드 정보를 기반으로 LogoSong 엔티티를 생성한 뒤 OpenAI API로 가사/비디오 가이드라인을 생성하여 저장하고, 업데이트된 전체 레코드를 반환합니다."
    )
    @ApiSuccessResponse(
        message = "가사/비디오 가이드라인 생성이 성공적으로 처리되었습니다.", 
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
        @Valid @RequestBody LogoSongCreateRequest request) {
        LogoSongResponse response = integratedLogoSongService.createLogoSongWithGuidesOnly(request);
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
            @Valid @RequestBody LogoSongCreateRequest request) {
        LogoSongResponse response = integratedLogoSongService.createLogoSongWithGeneration(request);
        return ApiResponse.successCreated(response);
    }

    @PostMapping("/{id}/generate-music")
    @Operation(summary = "음악 생성 트리거", 
               description = "기존 로고송에 대해 음악 생성을 시작합니다.\n\n" +
                       "처리 과정:\n" +
                       "- Suno API를 호출하여 음악 생성 요청\n" +
                       "- 비동기로 음악 생성 진행\n" +
                       "- 완료 시 음악 URL과 이미지 URL 자동 업데이트\n" +
                       "- 가사가 없는 경우 생성 불가\n\n" +
                       "상태 확인:\n" +
                       "- `/api/v1/logosongs/{id}/polling-status`: 실시간 상태 확인\n" +
                       "- `/api/v1/logosongs/{id}/quick-status`: 빠른 상태 확인")
    @ApiSuccessResponse(message = "음악 생성이 시작되었습니다.")
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.MUSIC_GENERATION_IN_PROGRESS,
            ErrorCode.LYRICS_GENERATION_FAILED
    })
    public ApiResponse<Void> generateMusic(
            @Parameter(description = "로고송 ID") @PathVariable Long id) {
        integratedLogoSongService.triggerMusicGeneration(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/generation-status")
    @Operation(summary = "음악 생성 상태 확인", description = "로고송의 음악 생성 진행 상태를 확인합니다.")
    @ApiSuccessResponse(message = "음악 생성 상태 조회가 성공적으로 처리되었습니다.", dataType = MusicGenerationResult.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND
    })
    public ApiResponse<MusicGenerationResult> getMusicGenerationStatus(
            @Parameter(description = "로고송 ID") @PathVariable Long id) {
        MusicGenerationResult result = integratedLogoSongService.checkMusicGenerationStatus(id);
        return ApiResponse.success(result);
    }

    

    @PostMapping("/{id}/regenerate-lyrics")
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
            @Valid @RequestBody LogoSongCreateRequest request) {
        LogoSongResponse updated = integratedLogoSongService.regenerateLyricsOnly(id, request);
        return ApiResponse.success(updated);
    }

    @PostMapping("/{id}/regenerate-video-guide")
    @Operation(summary = "비디오 가이드라인 (재)생성", 
               description = "기존 로고송 ID 기반으로 비디오 가이드라인만 생성/재생성합니다.\n\n" +
                       "처리 과정:\n" +
                       "- OpenAI API를 호출하여 새로운 비디오 가이드라인 생성\n" +
                       "- 기존 비디오 가이드라인을 새로운 것으로 교체\n" +
                       "- 가사는 유지됨\n" +
                       "- 음악 상태는 변경하지 않음\n\n" +
                       "사용 시나리오:\n" +
                       "- 기존 가사로 다른 영상 컨셉 시도\n" +
                       "- 비디오 가이드라인 품질 개선\n" +
                       "- 마케팅 방향성 변경")
    @ApiSuccessResponse(message = "비디오 가이드라인 (재)생성이 성공적으로 처리되었습니다.", dataType = LogoSongResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND,
            ErrorCode.LYRICS_GENERATION_FAILED
    })
    public ApiResponse<LogoSongResponse> regenerateVideoGuide(
            @Parameter(description = "로고송 ID") @PathVariable Long id,
            @Valid @RequestBody LogoSongCreateRequest request) {
        LogoSongResponse updated = integratedLogoSongService.regenerateVideoGuideOnly(id, request);
        return ApiResponse.success(updated);
    }

    // =========================== 웹 클라이언트 폴링 전용 엔드포인트 ===========================

    @GetMapping("/{id}/polling-status")
    @Operation(summary = "음악 생성 상태 폴링 (웹 클라이언트용)", 
               description = "웹 클라이언트가 주기적으로 호출하여 음악 생성 진행 상태를 확인합니다. " +
                       "진행률, 예상 완료 시간, 다음 폴링 간격 등을 제공합니다.")
    @ApiSuccessResponse(message = "음악 생성 상태 조회가 성공적으로 처리되었습니다.", dataType = MusicGenerationStatusResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND
    })
    public ApiResponse<MusicGenerationStatusResponse> getPollingStatus(
            @Parameter(description = "로고송 ID") @PathVariable Long id) {
        MusicGenerationStatusResponse status = pollingService.getPollingStatus(id);
        return ApiResponse.success(status);
    }

    @GetMapping("/{id}/quick-status")
    @Operation(summary = "빠른 상태 확인 (웹 클라이언트용)", 
               description = "DB만 확인하여 빠르게 상태를 반환합니다. Suno API 호출 없이 캐시된 상태만 반환합니다.")
    @ApiSuccessResponse(message = "빠른 상태 조회가 성공적으로 처리되었습니다.", dataType = MusicGenerationStatusResponse.class)
    @ApiErrorExamples({
            ErrorCode.LOGOSONG_NOT_FOUND
    })
    public ApiResponse<MusicGenerationStatusResponse> getQuickStatus(
            @Parameter(description = "로고송 ID") @PathVariable Long id) {
        MusicGenerationStatusResponse status = pollingService.getQuickPollingStatus(id);
        return ApiResponse.success(status);
    }

    // =========================== Suno API 콜백 엔드포인트 ===========================

    @PostMapping("/suno-callback")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Suno API 콜백 처리", 
               description = "Suno API에서 음악 생성 완료 시 호출하는 콜백 엔드포인트입니다.")
    @ApiSuccessResponse(message = "콜백 처리가 성공적으로 완료되었습니다.")
    @ApiErrorExamples({
            ErrorCode.INVALID_INPUT_VALUE,
            ErrorCode.SUNO_TASK_NOT_FOUND
    })
    public ApiResponse<Void> handleSunoCallback(
            @Parameter(description = "Suno API 콜백 데이터", required = true)
            @RequestBody Map<String, Object> callbackData) {
        log.info("Suno API 콜백 수신: {}", callbackData);
        
        try {
            // 콜백 데이터에서 taskId와 상태 정보 추출
            String taskId = (String) callbackData.get("taskId");
            String status = (String) callbackData.get("status");
            
            if (taskId == null) {
                log.error("콜백 데이터에 taskId가 없습니다: {}", callbackData);
                return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE);
            }
            
            // 서비스에서 콜백 처리
            // TODO: 실제 콜백 데이터 구조에 맞게 처리 로직 구현
            log.info("Suno 콜백 처리 완료: taskId={}, status={}", taskId, status);
            
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("Suno 콜백 처리 중 오류 발생", e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}