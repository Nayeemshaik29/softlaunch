package com.softlaunch.match.event;

import java.time.Instant;
import java.util.UUID;

public record SwipeRecordedEvent(UUID eventId, UUID swipeId, UUID swiperId, UUID targetId,
                                 String direction, Instant occurredAt) {

    public static SwipeRecordedEvent of(UUID swipeId, UUID swiperId, UUID targetId, String direction) {
        return new SwipeRecordedEvent(UUID.randomUUID(), swipeId, swiperId, targetId, direction, Instant.now());
    }
}