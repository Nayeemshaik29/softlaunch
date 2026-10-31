package com.softlaunch.chat.dto;

import java.time.Instant;

public record PresenceResponse(String userId, boolean online, Instant lastSeen) {
}