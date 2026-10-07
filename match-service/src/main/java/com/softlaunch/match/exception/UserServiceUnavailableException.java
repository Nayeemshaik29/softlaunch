package com.softlaunch.match.exception;

public class UserServiceUnavailableException extends RuntimeException {

    public UserServiceUnavailableException(Throwable cause) {
        super("User service is unavailable, please try again shortly", cause);
    }
}