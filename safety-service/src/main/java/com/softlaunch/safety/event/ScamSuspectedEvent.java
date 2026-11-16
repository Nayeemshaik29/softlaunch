package com.softlaunch.safety.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** From ai-service. No message text: we copy the chat ourselves as evidence (EvidenceCollector). */
public record ScamSuspectedEvent(@NotNull UUID eventId, @NotBlank String messageId, @NotNull UUID matchId,
                                 @NotNull UUID senderId, @NotNull UUID recipientId, @NotBlank String category,
                                 double confidence, @NotBlank String source, Set<String> signals,
                                 @NotNull Instant detectedAt) {
}
