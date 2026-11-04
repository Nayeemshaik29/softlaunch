package com.softlaunch.match.event;

import java.time.Instant;
import java.util.UUID;

/** Published by safety-service. active=true: blocked, active=false: unblocked. */
public record BlockChangedEvent(UUID eventId, UUID blockerId, UUID blockedId, boolean active, Instant occurredAt) {
}
