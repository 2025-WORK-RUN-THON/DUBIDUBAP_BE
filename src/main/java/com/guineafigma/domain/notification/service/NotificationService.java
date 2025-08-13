package com.guineafigma.domain.notification.service;

import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.service.UserService;
import com.guineafigma.domain.notification.dto.NotificationPatchRequestDTO;
import com.guineafigma.domain.notification.dto.NotificationResponseDTO;
import com.guineafigma.domain.notification.entity.Notification;
import com.guineafigma.domain.notification.enums.NotificationType;
import com.guineafigma.domain.notification.repository.NotificationRepository;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService memberService;

    public List<NotificationResponseDTO> getNotification(Long userId) {



        List<Notification> notifications = notificationRepository.findByReceiverId(userId);

        if (notifications.isEmpty()) {
            throw new BusinessException(ErrorCode.NOTIFICAITION_NOT_FOUND);
        }

        return notifications.stream()
                .map(NotificationResponseDTO::from)
                .toList();

    }

    @Transactional
    public void patchIsRead(Long userId, NotificationPatchRequestDTO patchRequestDTO) {

        Notification notification = notificationRepository.findByReceiverIdAndId(userId, patchRequestDTO.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICAITION_NOT_FOUND));

        notification.setIsRead(true);

    }

    public Notification postTempNotoification(Long userId) {
        User receiver = memberService.findById(userId);

        Notification notification = Notification.builder()
                .receiver(receiver)
                .message("임시 알림입니다.")
                .isRead(false)
                .type(NotificationType.SYSTEM) // 예: enum 값 TEMP
                .build();
        return notificationRepository.save(notification);
    }
}
