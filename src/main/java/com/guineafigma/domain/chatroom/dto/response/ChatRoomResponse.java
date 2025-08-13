package com.guineafigma.domain.chatroom.dto.response;

import com.guineafigma.domain.chatroom.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 응답")
// 채팅방 목록 조회 시 사용하는 응답 (채팅방 정보, 메시지 개수)
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "1")
    private Long id;

    @Schema(description = "채팅방 제목", example = "다이버리 해양 생물 도우미")
    private String title;

    @Schema(description = "메시지 개수", example = "1")
    private Integer messageCount;

    @Schema(description = "생성일시", example = "2025-01-01T10:00:00.000000")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-01T10:00:00.000000")
    private LocalDateTime updatedAt;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        // messages가 null이거나 빈 Map인 경우 0으로 처리
        int messageCount = (chatRoom.getMessages() != null) ? chatRoom.getMessages().size() : 0;
        
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .title(chatRoom.getTitle())
                .messageCount(messageCount)
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }
} 