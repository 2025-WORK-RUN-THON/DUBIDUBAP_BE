package com.guineafigma.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 메타데이터")
public class ChatRoomMetadata {
    
    @Schema(description = "마지막 메시지 ID", example = "msg_001")
    private String lastMessageId;
    
    @Schema(description = "메시지 개수", example = "1")
    private Integer messageCount;
    
    @Schema(description = "API 사용량 정보")
    private Usage usage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "API 사용량 정보")
    public static class Usage implements Serializable {
        
        @Schema(description = "프롬프트 토큰 수", example = "45")
        private Integer promptTokens;
        
        @Schema(description = "완성 토큰 수", example = "180")
        private Integer completionTokens;
        
        @Schema(description = "총 토큰 수", example = "225")
        private Integer totalTokens;
        
        @Schema(description = "사용된 모델", example = "gpt-3.5-turbo")
        private String model;
        
        @Schema(description = "비용", example = "0.0011")
        private Double cost;
    }
}