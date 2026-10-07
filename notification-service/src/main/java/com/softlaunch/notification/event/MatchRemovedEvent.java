package com.softlaunch.notification.event;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record MatchRemovedEvent(
        @NotNull UUID eventId,
        @NotNull UUID matchId,
        @NotNull UUID userAId,
        @NotNull UUID userBId,
        UUID removedBy,
        @NotNull Instant occurredAt) {
}