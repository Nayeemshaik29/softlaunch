package com.softlaunch.user.photo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A started upload. PENDING until confirmed; unconfirmed ones are cleaned up after pendingTtl. */
@Entity
@Table(name = "photo_uploads", indexes = {
        @Index(name = "photo_uploads_user_status", columnList = "userId,status"),
        @Index(name = "photo_uploads_expires", columnList = "status,expiresAt")})
public class PhotoUpload {

    public enum Status { PENDING, DONE }

    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false, length = 20)
    private String contentType;
    private long declaredBytes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant expiresAt;

    protected PhotoUpload() {
    }

    PhotoUpload(UUID userId, String contentType, long declaredBytes, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.contentType = contentType;
        this.declaredBytes = declaredBytes;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    /** Where the phone uploads the raw file. Raw files are never shown to anyone. */
    String pendingKey() {
        return "pending/" + userId + "/" + id;
    }

    /** Where the processed, metadata-free JPEG lives. */
    String photoKey() {
        return PhotoKeys.of(userId, id);
    }

    void markDone() { this.status = Status.DONE; }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getContentType() { return contentType; }
    public long getDeclaredBytes() { return declaredBytes; }
    public Status getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
}
