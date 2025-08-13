package com.guineafigma.domain.chatroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메시지 정보")
public class Message {
    
    @Schema(description = "메시지 ID", example = "msg_006")
    private String id;
    
    @Schema(description = "메시지 역할", example = "user")
    private String role;
    
    @Schema(description = "메시지 내용", example = "복어탕 끓이는 법을 알려주세요.")
    private String content;
    
    @Schema(description = "메시지 생성 시간", example = "2025-01-01T10:00:00.000000")
    private LocalDateTime timestamp;
    
    @Schema(description = "첨부파일 목록")
    private List<AttachmentDto> attachments;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "첨부파일 정보")
    public static class AttachmentDto {
        
        @Schema(description = "첨부파일 ID", example = "1")
        private Long id;
        
        @Schema(description = "파일 URL", example = "https://example_url_image.com")
        private String fileUrl;
        
        @Schema(description = "원본 파일명", example = "landscape.jpg")
        private String originalFilename;
    }
}