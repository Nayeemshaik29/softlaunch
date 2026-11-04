package com.softlaunch.chat.dto;

import com.softlaunch.chat.model.CaptureKind;

import java.time.Instant;

/** Pushed live on /topic/chats/{matchId}/alerts. The taker's own app ignores it (userId == me). */
public record CaptureAlert(String type, CaptureKind kind, String userId, Instant at) {
    public static CaptureAlert of(CaptureKind kind, String userId, Instant at) {
        return new CaptureAlert("CAPTURE", kind, userId, at);
    }
}
