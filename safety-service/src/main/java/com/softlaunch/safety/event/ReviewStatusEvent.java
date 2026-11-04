package com.softlaunch.safety.event;

import java.time.Instant;
import java.util.UUID;

public record ReviewStatusEvent(UUID eventId, UUID userId, boolean underReview, Instant occurredAt) {
    public static ReviewStatusEvent of(UUID userId, boolean underReview) {
        return new ReviewStatusEvent(UUID.randomUUID(), userId, underReview, Instant.now());
    }
}
