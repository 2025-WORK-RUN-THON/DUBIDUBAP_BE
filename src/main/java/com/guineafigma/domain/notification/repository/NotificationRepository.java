package com.guineafigma.domain.notification.repository;

import com.guineafigma.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverId(Long id);
    Optional<Notification> findByReceiverIdAndId(Long receiverId, Long notificationId);
}
