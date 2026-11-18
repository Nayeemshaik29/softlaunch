package com.softlaunch.user.session;

/** Unknown, expired, revoked or reused refresh token. Same answer for all: log in again. */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Your session has ended. Please log in again.");
    }
}
