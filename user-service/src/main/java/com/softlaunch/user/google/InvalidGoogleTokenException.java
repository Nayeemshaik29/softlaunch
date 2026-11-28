package com.softlaunch.user.google;

/** Bad, expired or foreign Google token. The message is for logs only and never contains the token. */
public class InvalidGoogleTokenException extends RuntimeException {
    public InvalidGoogleTokenException(String reason) {
        super(reason);
    }
}
