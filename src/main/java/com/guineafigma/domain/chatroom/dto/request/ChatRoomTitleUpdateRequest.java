package com.guineafigma.domain.chatroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅방 제목 변경 요청")
public class ChatRoomTitleUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 30, message = "제목은 30자 이하여야 합니다.")
    @Schema(description = "채팅방 제목", example = "복어탕 끼리는 방법")
    private String title;
}