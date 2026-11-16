package com.softlaunch.chat.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal event WITH the message text, read only by ai-service for scam screening.
 * Lives on its own topic (message.screening) that Kafka deletes after 24h, the same lifetime as the
 * chat message itself. message.sent stays text-free, so notification-service never sees text.
 */
public record MessageScreeningEvent(UUID eventId, String messageId, UUID matchId, UUID senderId, UUID recipientId,
                                    String text, Instant sentAt) {

    public static MessageScreeningEvent of(String messageId, UUID matchId, UUID senderId, UUID recipientId,
                                           String text, Instant sentAt) {
        return new MessageScreeningEvent(UUID.randomUUID(), messageId, matchId, senderId, recipientId, text, sentAt);
    }
}
