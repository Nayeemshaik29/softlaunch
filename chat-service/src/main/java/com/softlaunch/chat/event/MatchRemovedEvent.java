package com.softlaunch.chat.event;

import java.util.UUID;

public record MatchRemovedEvent(UUID eventId, UUID matchId) {
}
