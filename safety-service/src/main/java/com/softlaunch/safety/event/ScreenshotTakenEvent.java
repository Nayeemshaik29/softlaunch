package com.softlaunch.safety.event;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ScreenshotTakenEvent(@NotNull UUID eventId, @NotNull UUID matchId, @NotNull UUID takerId,
                                   @NotNull UUID otherUserId, @NotNull String kind, @NotNull Instant occurredAt) { }
