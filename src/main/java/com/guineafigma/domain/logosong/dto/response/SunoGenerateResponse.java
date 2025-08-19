package com.guineafigma.domain.logosong.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SunoGenerateResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("data")
    private SunoResponseData data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunoResponseData {
        @JsonProperty("taskId")
        private String taskId;
    }

    // Convenience method to get the task ID
    public String getId() {
        return data != null ? data.getTaskId() : null;
    }
}