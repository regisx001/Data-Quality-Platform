package com.regisx001.dQul.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.regisx001.dQul.notification.Notification;
import com.regisx001.dQul.notification.NotificationStatus;
import com.regisx001.dQul.notification.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientId(UUID recipientId);

    List<Notification> findByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByType(NotificationType type);

    long countByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);
}
