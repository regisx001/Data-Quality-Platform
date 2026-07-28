package com.regisx001.dQul.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.regisx001.dQul.domain.entities.Notification;
import com.regisx001.dQul.domain.enums.NotificationStatus;
import com.regisx001.dQul.domain.enums.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientId(UUID recipientId);

    List<Notification> findByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByType(NotificationType type);

    long countByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);
}
