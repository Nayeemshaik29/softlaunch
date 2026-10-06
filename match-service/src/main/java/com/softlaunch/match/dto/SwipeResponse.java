package com.softlaunch.match.dto;

import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;

import java.time.Instant;
import java.util.UUID;

public record SwipeResponse(
        UUID swipeId,
        UUID targetUserId,
        SwipeDirection direction,
        Instant createdAt,
        boolean matched,
        UUID matchId
) {
    public static SwipeResponse from(Swipe swipe, UUID matchId) {
        return new SwipeResponse(
                swipe.getId(),
                swipe.getTargetId(),
                swipe.getDirection(),
                swipe.getCreatedAt(),
                matchId != null,
                matchId
        );
    }
}