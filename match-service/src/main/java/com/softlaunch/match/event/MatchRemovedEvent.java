package com.softlaunch.match.event;

import java.time.Instant;
import java.util.UUID;

public record MatchRemovedEvent(UUID eventId, UUID matchId, UUID userAId, UUID userBId,
                                UUID removedBy, Instant occurredAt) {

    public static MatchRemovedEvent of(UUID matchId, UUID userAId, UUID userBId, UUID removedBy) {
        return new MatchRemovedEvent(UUID.randomUUID(), matchId, userAId, userBId, removedBy, Instant.now());
    }
}