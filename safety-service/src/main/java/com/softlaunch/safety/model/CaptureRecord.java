package com.softlaunch.safety.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/** One screenshot / screen recording of a chat. _id = the Kafka eventId, so a re-delivered event is ignored. */
@Document(collection = "capture_events")
public class CaptureRecord {

    @Id
    private String eventId;
    private String takerId;
    private String otherUserId;
    private String matchId;
    private String kind;
    private Instant occurredAt;
    private Instant expiresAt;       // TTL index

    protected CaptureRecord() { }

    public CaptureRecord(UUID eventId, UUID takerId, UUID otherUserId, UUID matchId, String kind,
                         Instant occurredAt, Instant expiresAt) {
        this.eventId = eventId.toString();
        this.takerId = takerId.toString();
        this.otherUserId = otherUserId.toString();
        this.matchId = matchId.toString();
        this.kind = kind;
        this.occurredAt = occurredAt;
        this.expiresAt = expiresAt;
    }
}
