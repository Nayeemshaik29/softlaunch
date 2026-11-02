package com.softlaunch.user.exception;

/** A Kafka message that can never be processed. Not retried: goes straight to the DLT. */
public class InvalidEventException extends RuntimeException {
    public InvalidEventException(String message) {
        super(message);
    }
}
