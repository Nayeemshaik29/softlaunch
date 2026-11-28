package com.softlaunch.match.dto;

import com.softlaunch.match.model.SwipeDirection;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwipeRequest(
        @NotNull UUID targetUserId,
        @NotNull SwipeDirection direction,
        @jakarta.validation.constraints.Size(max = 200) String note   // optional "note with a like" (costs 5 ❤)
) {
    public SwipeRequest(UUID targetUserId, SwipeDirection direction) {
        this(targetUserId, direction, null);
    }
}