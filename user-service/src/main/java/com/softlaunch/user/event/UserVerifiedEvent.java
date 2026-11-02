package com.softlaunch.user.event;

import java.time.Instant;
import java.util.UUID;

/** Published by verification-service. Carries no PII: only ids and a time. */
public record UserVerifiedEvent(UUID eventId, UUID userId, Instant verifiedAt) {
}
