package com.softlaunch.discovery.event;

import java.time.Instant;
import java.util.UUID;

public record ReviewStatusEvent(UUID eventId, UUID userId, boolean underReview, Instant occurredAt) {
}
