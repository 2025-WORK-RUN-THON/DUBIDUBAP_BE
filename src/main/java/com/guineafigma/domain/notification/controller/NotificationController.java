package com.guineafigma.domain.notification.controller;

import com.guineafigma.common.response.ApiResponse;
import com.guineafigma.domain.notification.dto.NotificationPatchRequestDTO;
import com.guineafigma.domain.notification.dto.NotificationResponseDTO;
import com.guineafigma.domain.notification.entity.Notification;
import com.guineafigma.domain.notification.service.NotificationService;

import com.guineafigma.global.config.SwaggerConfig.ApiErrorExamples;
import com.guineafigma.global.config.SwaggerConfig.ApiSuccessResponse;
import com.guineafigma.global.config.security.CustomUserPrincipal;
import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @ApiSuccessResponse(dataType = NotificationResponseDTO[].class)
    @ApiErrorExamples(value = {ErrorCode.NOTIFICAITION_NOT_FOUND, ErrorCode.AUTHENTICATION_REQUIRED})
    public ApiResponse<List<NotificationResponseDTO>> getNotification(@AuthenticationPrincipal CustomUserPrincipal userPrincipal) {
        List<NotificationResponseDTO> response = notificationService.getNotification(userPrincipal.getId());
        return ApiResponse.success(response);
    }

    @PatchMapping("/read")
    @Operation(summary = "알림 열람 여부 변경", description = "알림 열람 여부를 변경합니다.")
    @ApiSuccessResponse(dataType = Void.class)
    @ApiErrorExamples(value = {ErrorCode.NOTIFICAITION_NOT_FOUND})
    public void readNotification(@AuthenticationPrincipal CustomUserPrincipal userPrincipal, @Valid @RequestBody NotificationPatchRequestDTO requestDTO) {
        notificationService.patchIsRead(userPrincipal.getId(), requestDTO);
    }

    @PostMapping("/temp")
    @Operation(summary = "임시 알림 작성")
    @ApiSuccessResponse(dataType = Notification.class)
    @ApiErrorExamples(value = {ErrorCode.NOTIFICAITION_NOT_FOUND, ErrorCode.AUTHENTICATION_REQUIRED})
    public ApiResponse<Notification> postTempNotification(@AuthenticationPrincipal CustomUserPrincipal userPrincipal){
        Notification response = notificationService.postTempNotoification(userPrincipal.getId());

        return ApiResponse.success(response);
    }
}
