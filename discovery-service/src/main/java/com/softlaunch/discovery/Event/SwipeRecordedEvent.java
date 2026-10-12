package com.softlaunch.discovery.Event;

import java.util.UUID;

public record SwipeRecordedEvent(UUID eventId, UUID swiperId) {
}