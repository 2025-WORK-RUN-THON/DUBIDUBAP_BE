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
public class SunoGenerateRecordInfoResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("msg")
    private String msg;

    @JsonProperty("data")
    private Data data;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        @JsonProperty("taskId")
        private String taskId;

        @JsonProperty("status")
        private String status; // PENDING / TEXT_SUCCESS / FIRST_SUCCESS / SUCCESS / ...

        @JsonProperty("response")
        private Response response;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @JsonProperty("sunoData")
        private List<SunoData> sunoData;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SunoData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("audioUrl")
        private String audioUrl;

        @JsonProperty("streamAudioUrl")
        private String streamAudioUrl;

        @JsonProperty("imageUrl")
        private String imageUrl;

        @JsonProperty("title")
        private String title;

        @JsonProperty("tags")
        private String tags;

        @JsonProperty("duration")
        private Double duration;
    }
}

