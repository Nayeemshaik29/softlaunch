package com.softlaunch.match.dto;

import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;

import java.time.Instant;
import java.util.UUID;

public record SwipeResponse(UUID swipeId, UUID targetUserId, SwipeDirection direction, Instant createdAt) {

    public static SwipeResponse from(Swipe swipe) {
        return new SwipeResponse(swipe.getId(), swipe.getTargetId(), swipe.getDirection(), swipe.getCreatedAt());
    }
}