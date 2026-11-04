package com.softlaunch.chat.event;

import com.softlaunch.chat.model.CaptureKind;

import java.time.Instant;
import java.util.UUID;

/** No message text, no image: just who captured whose chat, and when. */
public record ScreenshotTakenEvent(UUID eventId, UUID matchId, UUID takerId, UUID otherUserId,
                                   CaptureKind kind, Instant occurredAt) {
    public static ScreenshotTakenEvent of(UUID matchId, UUID takerId, UUID otherUserId, CaptureKind kind, Instant at) {
        return new ScreenshotTakenEvent(UUID.randomUUID(), matchId, takerId, otherUserId, kind, at);
    }
}
