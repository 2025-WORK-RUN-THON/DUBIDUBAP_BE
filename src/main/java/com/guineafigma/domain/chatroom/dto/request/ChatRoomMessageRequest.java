package com.guineafigma.domain.chatroom.dto.request;

import org.springframework.lang.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 메시지 요청")
public class ChatRoomMessageRequest {

    @Schema(description = "채팅방 ID (없으면 새 채팅방 생성)", example = "1")
    @Nullable
    private Long chatRoomId;

    @NotBlank(message = "메시지는 필수입니다.")
    @Size(max = 150, message = "메시지는 150자 이하여야 합니다.")
    @Schema(description = "메시지 내용", example = "이 물고기가 뭔지 알 수 있을까요?")
    private String message;

    @Schema(description = "첨부 이미지 (선택사항)", type = "string", format = "binary")
    @Nullable
    private MultipartFile image;
} 