package com.softlaunch.notification.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_event_recipient",
                columnNames = {"source_event_id", "recipient_id"}),
        indexes = @Index(name = "idx_notification_recipient", columnList = "recipient_id, created_at")
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID recipientId, NotificationType type, String message, UUID referenceId, UUID sourceEventId) {
        this.recipientId = recipientId;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.sourceEventId = sourceEventId;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() { return id; }
    public UUID getRecipientId() { return recipientId; }
    public NotificationType getType() { return type; }
    public String getMessage() { return message; }
    public UUID getReferenceId() { return referenceId; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}