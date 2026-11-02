package com.softlaunch.safety.model;

import java.time.Instant;

/** A frozen copy of one chat message. The original disappears after 24h; this copy does not. */
public record EvidenceMessage(String senderId, String text, Instant sentAt) { }
