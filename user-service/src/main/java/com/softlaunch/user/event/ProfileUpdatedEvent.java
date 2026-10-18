package com.softlaunch.user.event;

import java.time.Instant;
import java.util.UUID;

public record ProfileUpdatedEvent(UUID eventId, UUID userId, Instant occurredAt) {

    public static ProfileUpdatedEvent of(UUID userId) {
        return new ProfileUpdatedEvent(UUID.randomUUID(), userId, Instant.now());
    }
}