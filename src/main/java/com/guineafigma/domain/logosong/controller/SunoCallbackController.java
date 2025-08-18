package com.guineafigma.domain.logosong.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.logosong.dto.request.SunoCallbackRequest;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
import com.guineafigma.domain.logosong.service.SunoApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Suno Callbacks", description = "Suno AI 콜백 엔드포인트 - Suno API에서 음악 생성 완료 시 호출되는 콜백 처리")
public class SunoCallbackController {

    private final SunoApiService sunoApiService;

    @PostMapping("/suno/music-generation")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Suno AI 음악 생성 완료 콜백", 
        description = "Suno AI API에서 로고송 음악 생성이 완료되면 자동으로 호출되는 웹훅 엔드포인트입니다. " +
                    "생성된 음악의 상태와 다운로드 URL을 수신하여 DB에 업데이트합니다. " +
                    "콜백 실패 시에도 200 응답을 반환하여 Suno의 재시도를 방지합니다."
    )
    public ApiResponse<Void> handleMusicGenerationCallback(
        @Parameter(description = "Suno API 콜백 요청 데이터 - 작업 ID, 상태, 음악 URL 등 포함", required = true)
        @RequestBody SunoCallbackRequest request) {
        try {
            log.info("Suno 음악 생성 콜백 수신: taskId={}, status={}", request.getId(), request.getStatus());
            
            MusicGenerationResult result = MusicGenerationResult.fromCallback(request);
            sunoApiService.handleMusicGenerationCallback(request.getId(), result);
            
            log.info("Suno 음악 생성 콜백 처리 완료: taskId={}", request.getId());
            return ApiResponse.success();
            
        } catch (Exception e) {
            log.error("Suno 음악 생성 콜백 처리 실패: taskId={}", request.getId(), e);
            // 콜백 실패해도 200 응답 (Suno API 재시도 방지)
            return ApiResponse.success();
        }
    }

    @PostMapping("/suno/lyrics-generation")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Suno AI 가사 생성 완료 콜백", 
        description = "Suno AI API에서 가사 생성이 완료되면 자동으로 호출되는 웹훅 엔드포인트입니다. " +
                    "현재는 예비 구현으로, 필요 시 가사 처리 로직을 추가할 수 있습니다. " +
                    "콜백 실패 시에도 200 응답을 반환합니다."
    )
    public ApiResponse<Void> handleLyricsGenerationCallback(
        @Parameter(description = "Suno API 가사 생성 콜백 요청 데이터 - 작업 ID, 상태 등 포함", required = true)
        @RequestBody SunoCallbackRequest request) {
        try {
            log.info("Suno 가사 생성 콜백 수신: taskId={}, status={}", request.getId(), request.getStatus());
            
            // 가사 생성 콜백 처리 로직 (필요시 구현)
            
            return ApiResponse.success();
            
        } catch (Exception e) {
            log.error("Suno 가사 생성 콜백 처리 실패: taskId={}", request.getId(), e);
            return ApiResponse.success();
        }
    }

    @GetMapping("/health")
    @Operation(
        summary = "콜백 엔드포인트 헬스체크", 
        description = "Suno AI 콜백 엔드포인트가 정상적으로 동작하는지 확인하는 헬스체크 엔드포인트입니다. " +
                    "Suno API에서 정기적으로 통신 상태를 확인하는 데 사용됩니다."
    )
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("OK");
    }
}