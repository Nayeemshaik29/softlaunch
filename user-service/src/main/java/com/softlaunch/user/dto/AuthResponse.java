package com.softlaunch.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * refreshToken is ONLY filled for the native apps (see session/NativeClients). Browsers get it as an HttpOnly cookie
 * instead and never see it in the body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, String refreshToken) {

    public AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {
        this(accessToken, tokenType, expiresInSeconds, null);
    }

    public AuthResponse withRefreshToken(String token) {
        return new AuthResponse(accessToken, tokenType, expiresInSeconds, token);
    }
}
