package com.softlaunch.discovery.event;

import java.util.UUID;

public record ProfileUpdatedEvent(UUID eventId, UUID userId) {
}