package com.softlaunch.user.deletion;

import java.time.Instant;
import java.util.UUID;

/** Sent to every service on topic user.deleted. Only the id: no email, no name (the account is gone). */
public record UserDeletedEvent(UUID eventId, UUID userId, Instant deletedAt) {

    public static UserDeletedEvent of(UUID userId) {
        return new UserDeletedEvent(UUID.randomUUID(), userId, Instant.now());
    }
}
