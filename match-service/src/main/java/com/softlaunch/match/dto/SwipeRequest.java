package com.softlaunch.match.dto;

import com.softlaunch.match.model.SwipeDirection;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SwipeRequest(
        @NotNull UUID targetUserId,
        @NotNull SwipeDirection direction
) {
}