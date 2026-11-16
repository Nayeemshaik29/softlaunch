package com.softlaunch.user.exception;

/** Same message whether or not the email exists, so the lock doesn't reveal which emails have accounts. */
public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException() {
        super("Too many failed attempts. Please try again later or reset your password.");
    }
}
