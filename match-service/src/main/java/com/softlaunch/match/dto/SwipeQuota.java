package com.softlaunch.match.dto;

import java.time.Instant;

/** limit = swipes per day, remaining = left today, resetsAt = next local midnight. */
public record SwipeQuota(int limit, int remaining, Instant resetsAt) {
}
