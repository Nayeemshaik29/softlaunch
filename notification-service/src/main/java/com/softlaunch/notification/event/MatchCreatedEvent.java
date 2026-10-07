package com.softlaunch.notification.event;

import java.time.Instant;
import java.util.UUID;

public record MatchCreatedEvent(UUID eventId, UUID matchId, UUID userAId, UUID userBId, Instant occurredAt) {
}