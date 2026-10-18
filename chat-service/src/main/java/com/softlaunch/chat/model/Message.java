package com.softlaunch.chat.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
public class Message {

    @Id
    private String id;

    private String matchId;
    private String senderId;
    private String text;
    private Instant sentAt;
    private Instant expiresAt;

    protected Message() {
    }

    public Message(String matchId, String senderId, String text, Instant sentAt, Instant expiresAt) {
        this.matchId = matchId;
        this.senderId = senderId;
        this.text = text;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public String getMatchId() { return matchId; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public Instant getSentAt() { return sentAt; }
    public Instant getExpiresAt() { return expiresAt; }
}