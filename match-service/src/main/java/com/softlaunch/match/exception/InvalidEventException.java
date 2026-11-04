package com.softlaunch.match.exception;

/** A Kafka message that can never be processed: not retried, goes to the DLT. */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
