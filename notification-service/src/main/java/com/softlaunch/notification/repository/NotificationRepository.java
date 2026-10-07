package com.softlaunch.notification.repository;

import com.softlaunch.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsBySourceEventIdAndRecipientId(UUID sourceEventId, UUID recipientId);

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);
}