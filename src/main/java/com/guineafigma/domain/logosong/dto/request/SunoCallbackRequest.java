package com.guineafigma.domain.logosong.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Suno AI 콜백 요청 데이터 - Suno API에서 음악/가사 생성 완료 시 전송되는 데이터")
public class SunoCallbackRequest {

    @JsonProperty("id")
    @Schema(description = "Suno 작업 ID - 음악 생성 요청 시 할당된 고유 식별자", example = "550e8400-e29b-41d4-a716-446655440000")
    private String id;

    @JsonProperty("title")
    @Schema(description = "생성된 음악 제목", example = "카페 뒤비뒤밥 로고송")
    private String title;

    @JsonProperty("image_url")
    @Schema(description = "음악과 함께 생성된 이미지 URL", example = "https://cdn1.suno.ai/image_550e8400.jpeg")
    private String imageUrl;

    @JsonProperty("lyric")
    @Schema(description = "생성된 가사 내용", example = "카페 뒤비뒤밥에서\n따뜻한 커피 한 잔\n달콤한 케이크와 함께\n행복한 시간")
    private String lyric;

    @JsonProperty("audio_url")
    @Schema(description = "생성된 음악 파일 다운로드 URL", example = "https://cdn1.suno.ai/550e8400-e29b-41d4-a716-446655440000.mp3")
    private String audioUrl;

    @JsonProperty("video_url")
    @Schema(description = "생성된 비디오 파일 URL (있는 경우)", example = "https://cdn1.suno.ai/550e8400-e29b-41d4-a716-446655440000.mp4")
    private String videoUrl;

    @JsonProperty("created_at")
    @Schema(description = "음악 생성 완료 시간", example = "2024-01-15T10:30:00.000Z")
    private String createdAt;

    @JsonProperty("model_name")
    @Schema(description = "사용된 Suno AI 모델명", example = "chirp-v3.5")
    private String modelName;

    @JsonProperty("status")
    @Schema(description = "음악 생성 상태", example = "complete", allowableValues = {"queued", "processing", "complete", "error"})
    private String status;

    @JsonProperty("gpt_description_prompt")
    @Schema(description = "GPT가 생성한 음악 설명 프롬프트", example = "Upbeat acoustic guitar melody for a cozy cafe atmosphere")
    private String gptDescriptionPrompt;

    @JsonProperty("prompt")
    @Schema(description = "음악 생성에 사용된 최종 프롬프트", example = "Acoustic folk song about a warm cozy cafe")
    private String prompt;

    @JsonProperty("type")
    @Schema(description = "생성 타입", example = "music", allowableValues = {"music", "lyrics"})
    private String type;

    @JsonProperty("tags")
    @Schema(description = "음악 태그", example = "acoustic, folk, cafe, cozy")
    private String tags;

    @JsonProperty("duration")
    @Schema(description = "음악 길이 (초)", example = "25.5")
    private Double duration;

    @JsonProperty("error_type")
    @Schema(description = "에러 타입 (에러 발생시)", example = "content_policy_violation")
    private String errorType;

    @JsonProperty("error_message")
    @Schema(description = "에러 메시지 (에러 발생시)", example = "Content violates usage policy")
    private String errorMessage;
}