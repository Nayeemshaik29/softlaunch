package com.softlaunch.discovery.event;

import java.util.UUID;

public record SwipeRecordedEvent(UUID eventId, UUID swiperId) {
}