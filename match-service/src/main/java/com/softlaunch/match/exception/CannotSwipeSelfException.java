package com.softlaunch.match.exception;

public class CannotSwipeSelfException extends RuntimeException {

    public CannotSwipeSelfException() {
        super("You cannot swipe on yourself");
    }
}