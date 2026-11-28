package com.softlaunch.match.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "swipes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_swipe_swiper_target",
                columnNames = {"swiper_id", "target_id"}),
        indexes = @Index(
                name = "idx_swipe_target_swiper",
                columnList = "target_id, swiper_id")
)
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "swiper_id", nullable = false)
    private UUID swiperId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SwipeDirection direction;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Optional note sent with a like. Shown to the person liked; they can only reply by liking back. */
    @Column(length = 200)
    private String note;

    protected Swipe() {
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Swipe(UUID swiperId, UUID targetId, SwipeDirection direction) {
        this.swiperId = swiperId;
        this.targetId = targetId;
        this.direction = direction;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSwiperId() {
        return swiperId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public SwipeDirection getDirection() {
        return direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}