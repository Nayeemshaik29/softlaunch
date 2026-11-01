package com.softlaunch.notification.event;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MessageSentEvent(
        @NotNull UUID eventId,
        @NotNull UUID matchId,
        @NotNull UUID senderId,
        @NotNull UUID recipientId,
        boolean recipientOnline) {
}
