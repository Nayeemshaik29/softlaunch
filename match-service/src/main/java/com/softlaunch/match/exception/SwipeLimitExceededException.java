package com.softlaunch.match.exception;

import java.time.Instant;

public class SwipeLimitExceededException extends RuntimeException {

    private final Instant resetsAt;

    public SwipeLimitExceededException(int limit, Instant resetsAt) {
        super("Daily swipe limit of " + limit + " reached. Come back tomorrow!");
        this.resetsAt = resetsAt;
    }

    public Instant getResetsAt() {
        return resetsAt;
    }
}
