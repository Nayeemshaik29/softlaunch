package com.softlaunch.ai.scam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/** From chat-service (topic message.screening). Contains the message TEXT: never log it. */
public record MessageScreeningEvent(@NotNull UUID eventId, @NotBlank String messageId, @NotNull UUID matchId,
                                    @NotNull UUID senderId, @NotNull UUID recipientId, String text,
                                    @NotNull Instant sentAt) {
}
