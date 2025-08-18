package com.guineafigma.domain.assistant.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.GuidesResponse;
import com.guineafigma.domain.assistant.service.AssistantGuidesService;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.SwaggerConfig.ApiSuccessResponse;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant", description = "AI 어시스턴트 API - OpenAI를 활용한 가사/비디오 가이드라인 생성 서비스")
public class AssistantController {

    private final AssistantGuidesService guidesGenerationService;

    @PostMapping("/guides")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "가사/비디오 가이드라인 생성", 
        description = "브랜드 정보를 기반으로 OpenAI API를 사용해 로고송 가사와 비디오 가이드라인을 생성합니다. " +
                    "서비스명, 슬로건, 업종, 타겟고객, 분위기 등을 종합적으로 분석하여 맞춤형 콘텐츠를 제공합니다."
    )
    @ApiSuccessResponse(
        message = "가사/비디오 가이드라인이 성공적으로 생성되었습니다.", 
        dataType = GuidesResponse.class
    )
    @ApiErrorExamples({
        ErrorCode.VALIDATION_ERROR,
        ErrorCode.SERVICE_NAME_REQUIRED,
        ErrorCode.LYRICS_GENERATION_FAILED,
        ErrorCode.INTERNAL_SERVER_ERROR
    })
    public ApiResponse<GuidesResponse> generateGuides(
        @Parameter(description = "로고송 생성 요청 정보 - 브랜드 및 음악 스타일 정보 포함", required = true)
        @Valid @RequestBody LogoSongCreateRequest request) {
        GuidesResponse guides = guidesGenerationService.generateGuides(request);
        return ApiResponse.success(guides);
    }

    // 향후 확장 예정: FastAPI 연동 (Whisper/임베딩/유튜브 분석 파이프라인) 엔드포인트
}


