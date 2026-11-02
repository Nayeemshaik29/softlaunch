package com.softlaunch.verification.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    protected OutboxEvent() { }

    public static OutboxEvent of(String topic, String messageKey, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.id = UUID.randomUUID();
        e.topic = topic;
        e.messageKey = messageKey;
        e.payload = payload;
        e.createdAt = Instant.now();
        return e;
    }

    public void markPublished() {
        this.publishedAt = Instant.now();
        this.attempts++;
    }

    public void recordFailure() {
        this.attempts++;
    }

    public UUID getId() { return id; }
    public String getTopic() { return topic; }
    public String getMessageKey() { return messageKey; }
    public String getPayload() { return payload; }
    public int getAttempts() { return attempts; }
}
