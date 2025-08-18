package com.guineafigma.domain.logosong.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.logosong.dto.request.SunoCallbackRequest;
import com.guineafigma.domain.logosong.dto.response.MusicGenerationResult;
import com.guineafigma.domain.logosong.service.SunoApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Suno Callbacks", description = "Suno API 콜백 처리")
public class SunoCallbackController {

    private final SunoApiService sunoApiService;

    @PostMapping("/suno/music-generation")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Suno 음악 생성 콜백", description = "Suno API에서 음악 생성 완료 시 호출되는 콜백 엔드포인트")
    public ApiResponse<Void> handleMusicGenerationCallback(@RequestBody SunoCallbackRequest request) {
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
    @Operation(summary = "Suno 가사 생성 콜백", description = "Suno API에서 가사 생성 완료 시 호출되는 콜백 엔드포인트")
    public ApiResponse<Void> handleLyricsGenerationCallback(@RequestBody SunoCallbackRequest request) {
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
    @Operation(summary = "콜백 헬스체크", description = "콜백 엔드포인트 상태 확인")
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("OK");
    }
}