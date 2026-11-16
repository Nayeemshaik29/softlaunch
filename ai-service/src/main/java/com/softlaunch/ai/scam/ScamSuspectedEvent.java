package com.softlaunch.ai.scam;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * To safety-service (topic scam.suspected). Deliberately has NO message text: safety copies the chat
 * itself as evidence, under its own retention rules.
 * source = "ai" (LLM confirmed) or "rules" (LLM unavailable, but the rules were very sure).
 */
public record ScamSuspectedEvent(UUID eventId, String messageId, UUID matchId, UUID senderId, UUID recipientId,
                                 String category, double confidence, String source, Set<String> signals,
                                 Instant detectedAt) {
}
