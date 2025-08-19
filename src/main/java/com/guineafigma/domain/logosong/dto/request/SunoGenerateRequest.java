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

    @JsonProperty("customMode")
    private Boolean customMode;

    @JsonProperty("instrumental")
    private Boolean instrumental;

    @JsonProperty("model")
    private String model;

    @JsonProperty("callBackUrl")
    private String callBackUrl;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("style")
    private String style;

    @JsonProperty("title")
    private String title;

    @JsonProperty("negativeTags")
    private String negativeTags;

    @JsonProperty("vocalGender")
    private String vocalGender;

    @JsonProperty("styleWeight")
    private Double styleWeight;

    @JsonProperty("weirdnessConstraint")
    private Double weirdnessConstraint;

    @JsonProperty("audioWeight")
    private Double audioWeight;

    public static SunoGenerateRequest of(String prompt, String style, String title, String model, String callBackUrl) {
        return SunoGenerateRequest.builder()
                .customMode(true)
                .instrumental(false)
                .model(model != null ? model : "V3_5")
                .callBackUrl(callBackUrl)
                .prompt(prompt)
                .style(style)
                .title(title)
                .vocalGender("m")
                .styleWeight(0.65)
                .weirdnessConstraint(0.65)
                .audioWeight(0.65)
                .build();
    }

    // Backward compatibility for existing tests/usages without callback URL
    public static SunoGenerateRequest of(String prompt, String style, String title, String model) {
        return of(prompt, style, title, model, null);
    }
}