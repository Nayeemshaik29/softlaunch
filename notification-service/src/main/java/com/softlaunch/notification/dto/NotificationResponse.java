package com.softlaunch.notification.dto;

import com.softlaunch.notification.model.Notification;
import com.softlaunch.notification.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, String message, UUID referenceId,
                                   boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getMessage(), n.getReferenceId(),
                n.isRead(), n.getCreatedAt());
    }
}