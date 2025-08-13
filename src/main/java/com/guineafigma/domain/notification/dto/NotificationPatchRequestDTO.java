package com.guineafigma.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class NotificationPatchRequestDTO {
    @Schema(description = "열람 상태를 변경하고자 하는 알림의 id를 보내주세요", example = "123")
    @NotNull
    private Long id;
}
