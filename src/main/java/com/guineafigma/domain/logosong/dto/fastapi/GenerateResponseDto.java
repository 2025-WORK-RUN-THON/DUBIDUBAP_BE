package com.guineafigma.domain.logosong.dto.fastapi;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class GenerateResponseDto {
    private String master_prompt;
    private String suno_prompt;
    private List<Map<String, String>> examples;
    private Analysis analysis;
    private SunoRequestBase suno_request; // alias
    private SunoRequestBase sunoRequestBase; // explicit
    private String requestId;

    @Data
    public static class Analysis {
        private List<String> trends;
        private List<Map<String, String>> examples;
        private MusicSummary musicSummary;
        private Map<String, Double> emotionHint;
        private List<String> hooks;
    }

    @Data
    public static class MusicSummary {
        private Double bpm;
        private String key;
        private String mode;
    }

    @Data
    public static class SunoRequestBase {
        private boolean customMode;
        private Boolean instrumental;
        private String model;
        private String callBackUrl;
        private String prompt; // expected empty
        private String style;
        private String title;
        private String negativeTags;
        private String vocalGender; // m|f|null
        private Double styleWeight;
        private Double weirdnessConstraint;
        private Double audioWeight;
    }
}


