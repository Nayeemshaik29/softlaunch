package com.softlaunch.user.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {
}