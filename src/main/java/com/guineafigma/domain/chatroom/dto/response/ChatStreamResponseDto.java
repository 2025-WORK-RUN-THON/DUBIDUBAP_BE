package com.guineafigma.domain.chatroom.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 정의되지 않은 JSON 필드는 무시
public class ChatStreamResponseDto implements Serializable {

    private String id; // 스트림 고유 ID
    private List<Choice> choices; // 응답 선택지 목록

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        private Delta delta; // 실제 변경 내용
        private Integer index; // 응답 후보 인덱스
        @JsonProperty("finish_reason")
        private String finishReason; // 스트림 종료 이유

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Delta {

            private String content; // 텍스트 조각
        }
    }
}
