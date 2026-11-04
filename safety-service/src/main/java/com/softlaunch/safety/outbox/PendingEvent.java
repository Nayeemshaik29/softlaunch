package com.softlaunch.safety.outbox;

import java.time.Instant;

/**
 * "Please publish this" stored INSIDE the document it belongs to.
 * Mongo (single node) has no multi-document transactions, but a write to ONE document is atomic:
 * the state change and its pending event are saved together, or not at all.
 */
public record PendingEvent(String eventId, String topic, String key, String payload, Instant createdAt) { }
