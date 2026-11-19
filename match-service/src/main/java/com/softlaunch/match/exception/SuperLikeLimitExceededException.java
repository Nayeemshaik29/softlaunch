package com.softlaunch.match.exception;

import java.time.Instant;

public class SuperLikeLimitExceededException extends RuntimeException {
    private final Instant resetsAt;
    private final boolean plus;

    public SuperLikeLimitExceededException(int limit, Instant resetsAt, boolean plus) {
        super(limit == 1 ? "That's your super like for today." : "That's your " + limit + " super likes for today.");
        this.resetsAt = resetsAt;
        this.plus = plus;
    }

    public Instant getResetsAt() { return resetsAt; }
    public boolean isPlus() { return plus; }
}
