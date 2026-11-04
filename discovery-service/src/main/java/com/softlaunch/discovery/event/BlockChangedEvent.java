package com.softlaunch.discovery.event;

import java.time.Instant;
import java.util.UUID;

public record BlockChangedEvent(UUID eventId, UUID blockerId, UUID blockedId, boolean active, Instant occurredAt) {
}
