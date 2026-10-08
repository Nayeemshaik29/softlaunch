package com.softlaunch.discovery.dto;

import java.util.UUID;

public record NearbyUserResponse(UUID userId, int distanceKm) {
}