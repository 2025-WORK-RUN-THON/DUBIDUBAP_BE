package com.guineafigma.domain.logosong.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoGenerateRequest {

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("make_instrumental")
    private Boolean makeInstrumental;

    @JsonProperty("wait_audio")
    private Boolean waitAudio;

    @JsonProperty("model")
    private String model;

    @JsonProperty("tags")
    private String tags;

    @JsonProperty("title")
    private String title;

    public static SunoGenerateRequest of(String prompt, String tags, String title, String model) {
        return SunoGenerateRequest.builder()
                .prompt(prompt)
                .makeInstrumental(false)
                .waitAudio(false)
                .model(model != null ? model : "v4")
                .tags(tags)
                .title(title)
                .build();
    }
}