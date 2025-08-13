package com.guineafigma.domain.chatroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OpenAI API 응답")
public class OpenAIResponse {
    
    @Schema(description = "생성된 컨텐츠", example = "안녕하세요! 도움이 필요하시면 언제든 말씀해주세요.")
    private String content;
    
    @Schema(description = "입력 토큰 수", example = "150")
    private int promptTokens;
    
    @Schema(description = "출력 토큰 수", example = "50")
    private int completionTokens;
    
    @Schema(description = "총 토큰 수", example = "200")
    private int totalTokens;
    
    @Schema(description = "사용된 모델", example = "gpt-4o-mini")
    private String model;
    
    @Schema(description = "API 호출 비용 (USD)", example = "0.0032")
    private double cost;
}