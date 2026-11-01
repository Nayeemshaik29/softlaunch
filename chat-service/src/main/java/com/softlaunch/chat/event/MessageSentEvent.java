package com.softlaunch.chat.event;

import java.time.Instant;
import java.util.UUID;

public record MessageSentEvent(UUID eventId, String messageId, UUID matchId, UUID senderId,
                               UUID recipientId, boolean recipientOnline, Instant sentAt) {

    public static MessageSentEvent of(String messageId, UUID matchId, UUID senderId,
                                      UUID recipientId, boolean recipientOnline, Instant sentAt) {
        return new MessageSentEvent(UUID.randomUUID(), messageId, matchId, senderId,
                recipientId, recipientOnline, sentAt);
    }
}
