package com.guineafigma.domain.assistant.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.GuidesResponse;
import com.guineafigma.domain.assistant.service.AssistantGuidesService;
import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.SwaggerConfig.ApiSuccessResponse;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
@Tag(name = "Assistant", description = "AI 어시스턴트(챗봇) 도메인")
public class AssistantController {

    private final AssistantGuidesService guidesGenerationService;

    @PostMapping("/guides")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "가사/비디오 가이드라인 생성", description = "사용자 입력을 기반으로 가사와 비디오 가이드라인을 생성합니다.")
    @ApiSuccessResponse(message = "생성 완료", dataType = GuidesResponse.class)
    @ApiErrorExamples({ ErrorCode.VALIDATION_ERROR, ErrorCode.INTERNAL_SERVER_ERROR })
    public ApiResponse<GuidesResponse> generateGuides(@Valid @RequestBody LogoSongCreateRequest request) {
        GuidesResponse guides = guidesGenerationService.generateGuides(request);
        return ApiResponse.success(guides);
    }

    // TODO: FastAPI 연동 (Whisper/임베딩/유튜브 분석 파이프라인) 엔드포인트 추가 예정
}


