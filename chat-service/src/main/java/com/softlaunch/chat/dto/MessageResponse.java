package com.softlaunch.chat.dto;

import com.softlaunch.chat.model.Message;

import java.time.Instant;

public record MessageResponse(String id, String matchId, String senderId, String text,
                              Instant sentAt, Instant expiresAt) {

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getMatchId(), m.getSenderId(), m.getText(),
                m.getSentAt(), m.getExpiresAt());
    }
}