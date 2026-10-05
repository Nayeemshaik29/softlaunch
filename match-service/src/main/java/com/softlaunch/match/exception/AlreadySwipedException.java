package com.softlaunch.match.exception;

import java.util.UUID;

public class AlreadySwipedException extends RuntimeException {

    public AlreadySwipedException(UUID targetUserId) {
        super("You already swiped on user: " + targetUserId);
    }
}