package com.guineafigma.domain.notification.dto;

import com.guineafigma.domain.notification.entity.Notification;
import com.guineafigma.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class NotificationResponseDTO {

    @Schema(description = "알림 id", example = "1")
    private Long id;

    @Schema(description = "타입", example = "시스템")
    private NotificationType type;

    @Schema(description = "메시지", example = "업데이트 완료!ㅂ")
    private String message;

    @Schema(description = "열람 여부", example = "true")
    private Boolean isRead;

    @Schema(description = "메시지가 만들어진 시간", example = "yyyy-MM-dd'T'HH:mm:ss)")
    private LocalDateTime createdAt;

    public static NotificationResponseDTO from(Notification notification) {
        String safeMessage = (notification.getMessage() != null) ? notification.getMessage() : "메시지가 존재하지 않습니다";

        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(safeMessage)
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
