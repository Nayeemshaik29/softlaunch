package com.softlaunch.user.exception;

/** Unknown, expired or already used deletion link. One message for all three: no hints for guessers. */
public class InvalidDeletionLinkException extends RuntimeException {
    public InvalidDeletionLinkException() {
        super("This link has expired or was already used. Ask for a new one.");
    }
}
