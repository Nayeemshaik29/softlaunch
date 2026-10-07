package com.softlaunch.match.exception;

import java.util.UUID;

public class TargetUserNotFoundException extends RuntimeException {

    public TargetUserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}