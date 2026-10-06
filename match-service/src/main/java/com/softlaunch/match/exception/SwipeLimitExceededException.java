package com.softlaunch.match.exception;

public class SwipeLimitExceededException extends RuntimeException {

    public SwipeLimitExceededException(int limit) {
        super("Daily swipe limit of " + limit + " reached. Come back tomorrow!");
    }
}