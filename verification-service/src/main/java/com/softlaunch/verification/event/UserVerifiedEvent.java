package com.softlaunch.verification.event;

import java.time.Instant;
import java.util.UUID;

/** No name, no DOB: Kafka messages are stored on disk and read by many services. */
public record UserVerifiedEvent(UUID eventId, UUID userId, Instant verifiedAt) {
    public static UserVerifiedEvent of(UUID userId) {
        return new UserVerifiedEvent(UUID.randomUUID(), userId, Instant.now());
    }
}
