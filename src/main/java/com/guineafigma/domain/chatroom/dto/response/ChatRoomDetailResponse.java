package com.guineafigma.domain.chatroom.dto.response;

import com.guineafigma.domain.chatroom.dto.ChatRoomMetadata;
import com.guineafigma.domain.chatroom.dto.Message;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 상세 정보 응답")
// 채팅방 정보, 전체 메시지 목록, API 사용량 정보 
public class ChatRoomDetailResponse {
    
    @Schema(description = "채팅방 정보")
    private ChatRoomResponse chatRoom;
    
    @Schema(description = "메시지 목록 (사용자 메시지 + AI 응답)")
    private List<Message> messages;
    
    @Schema(description = "API 사용량 정보")
    private ChatRoomMetadata.Usage usage;
}