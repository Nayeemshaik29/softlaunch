package com.softlaunch.match.event;

import java.time.Instant;
import java.util.UUID;

public record MatchCreatedEvent(
        UUID eventId,
        UUID matchId,
        UUID userAId,
        UUID userBId,
        Instant occurredAt
) {
    public static MatchCreatedEvent of(UUID matchId, UUID userAId, UUID userBId) {
        return new MatchCreatedEvent(UUID.randomUUID(), matchId, userAId, userBId, Instant.now());
    }
}