package com.softlaunch.safety.model;

import com.softlaunch.safety.outbox.HasPendingEvent;
import com.softlaunch.safety.outbox.PendingEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/** One document per direction (A blocks B). Unblock keeps the document with active=false. */
@Document(collection = "blocks")
public class Block implements HasPendingEvent {

    @Id
    private String id;
    private String blockerId;
    private String blockedId;
    private boolean active;
    private Instant blockedAt;
    private Instant updatedAt;
    private PendingEvent pendingEvent;

    protected Block() { }

    public static Block create(UUID blockerId, UUID blockedId) {
        Block b = new Block();
        b.blockerId = blockerId.toString();
        b.blockedId = blockedId.toString();
        return b;
    }

    public void activate(PendingEvent event) {
        this.active = true;
        this.blockedAt = Instant.now();
        this.updatedAt = blockedAt;
        this.pendingEvent = event;
    }

    public void deactivate(PendingEvent event) {
        this.active = false;
        this.updatedAt = Instant.now();
        this.pendingEvent = event;
    }

    @Override public String outboxId() { return id; }
    @Override public PendingEvent getPendingEvent() { return pendingEvent; }

    public String getId() { return id; }
    public UUID getBlockerId() { return UUID.fromString(blockerId); }
    public UUID getBlockedId() { return UUID.fromString(blockedId); }
    public boolean isActive() { return active; }
    public Instant getBlockedAt() { return blockedAt; }
}
