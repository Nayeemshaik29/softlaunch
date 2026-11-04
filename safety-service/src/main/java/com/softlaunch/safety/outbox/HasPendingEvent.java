package com.softlaunch.safety.outbox;

/** Any document that can carry an embedded outbox event. */
public interface HasPendingEvent {
    String outboxId();
    PendingEvent getPendingEvent();
}
