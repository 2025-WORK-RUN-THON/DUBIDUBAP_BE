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
public class SunoGenerateResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("clips")
    private List<SunoClip> clips;

    @JsonProperty("metadata")
    private SunoMetadata metadata;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunoClip {
        @JsonProperty("id")
        private String id;

        @JsonProperty("video_url")
        private String videoUrl;

        @JsonProperty("audio_url")
        private String audioUrl;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("image_large_url")
        private String imageLargeUrl;

        @JsonProperty("major_model_version")
        private String majorModelVersion;

        @JsonProperty("model_name")
        private String modelName;

        @JsonProperty("metadata")
        private SunoClipMetadata metadata;

        @JsonProperty("is_liked")
        private Boolean isLiked;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("display_name")
        private String displayName;

        @JsonProperty("handle")
        private String handle;

        @JsonProperty("is_handle_updated")
        private Boolean isHandleUpdated;

        @JsonProperty("is_trashed")
        private Boolean isTrashed;

        @JsonProperty("reaction")
        private String reaction;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("status")
        private String status;

        @JsonProperty("title")
        private String title;

        @JsonProperty("play_count")
        private Integer playCount;

        @JsonProperty("upvote_count")
        private Integer upvoteCount;

        @JsonProperty("is_public")
        private Boolean isPublic;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunoClipMetadata {
        @JsonProperty("tags")
        private String tags;

        @JsonProperty("prompt")
        private String prompt;

        @JsonProperty("gpt_description_prompt")
        private String gptDescriptionPrompt;

        @JsonProperty("audio_prompt_id")
        private String audioPromptId;

        @JsonProperty("history")
        private String history;

        @JsonProperty("concat_history")
        private String concatHistory;

        @JsonProperty("type")
        private String type;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("refund_credits")
        private Boolean refundCredits;

        @JsonProperty("stream")
        private Boolean stream;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunoMetadata {
        @JsonProperty("tags")
        private String tags;

        @JsonProperty("prompt")
        private String prompt;

        @JsonProperty("gpt_description_prompt")
        private String gptDescriptionPrompt;

        @JsonProperty("audio_prompt_id")
        private String audioPromptId;

        @JsonProperty("history")
        private String history;

        @JsonProperty("concat_history")
        private String concatHistory;

        @JsonProperty("type")
        private String type;

        @JsonProperty("duration")
        private Double duration;

        @JsonProperty("refund_credits")
        private Boolean refundCredits;

        @JsonProperty("stream")
        private Boolean stream;
    }
}