package com.softlaunch.safety.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A STATE event: "blocker -> blocked is now active / not active". Because it carries the full current state,
 * it's safe for a newer event to replace an older unsent one.
 */
public record BlockChangedEvent(UUID eventId, UUID blockerId, UUID blockedId, boolean active, Instant occurredAt) {

    public static BlockChangedEvent of(UUID blockerId, UUID blockedId, boolean active) {
        return new BlockChangedEvent(UUID.randomUUID(), blockerId, blockedId, active, Instant.now());
    }

    /** Same key for A->B and B->A, so all events about this pair land on one partition, in order. */
    public String pairKey() {
        return blockerId.compareTo(blockedId) < 0 ? blockerId + ":" + blockedId : blockedId + ":" + blockerId;
    }
}
