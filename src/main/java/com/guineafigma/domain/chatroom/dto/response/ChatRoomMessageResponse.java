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
@Schema(description = "채팅방 메시지 전송 응답")
// 메세지를 보낼 때 사용하는 응답 (채팅방 정보, 새로 추가된 메시지만, API 사용량 정보)
public class ChatRoomMessageResponse {
    
    @Schema(description = "채팅방 ID")
    private Long chatRoomId;
    
    @Schema(description = "채팅방 제목")
    private String title;
    
    @Schema(description = "새로 추가된 메시지 목록 (사용자 메시지 + AI 응답)")
    private List<Message> newMessages;
    
    @Schema(description = "API 사용량 정보")
    private ChatRoomMetadata.Usage usage;
}