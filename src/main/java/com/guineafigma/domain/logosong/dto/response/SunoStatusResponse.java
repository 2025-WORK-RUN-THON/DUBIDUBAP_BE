package com.guineafigma.domain.logosong.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoStatusResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("lyric")
    private String lyric;

    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonProperty("video_url")
    private String videoUrl;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("gpt_description_prompt")
    private String gptDescriptionPrompt;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("type")
    private String type;

    @JsonProperty("tags")
    private String tags;

    @JsonProperty("duration")
    private Double duration;

    @JsonProperty("error_type")
    private String errorType;

    @JsonProperty("error_message")
    private String errorMessage;

    public boolean isCompleted() {
        return "complete".equals(status);
    }

    public boolean isFailed() {
        return "error".equals(status) || errorType != null;
    }

    public boolean isProcessing() {
        return "submitted".equals(status) || "queued".equals(status) || "streaming".equals(status);
    }
}