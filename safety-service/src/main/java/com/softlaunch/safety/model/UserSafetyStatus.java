package com.softlaunch.safety.model;

import com.softlaunch.safety.outbox.HasPendingEvent;
import com.softlaunch.safety.outbox.PendingEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/** One document per reported user. "Under review" = moderators must look at this person. */
@Document(collection = "user_safety_status")
public class UserSafetyStatus implements HasPendingEvent {

    @Id
    private String userId;
    private boolean underReview;
    private String reviewReason;
    private Instant underReviewSince;
    private PendingEvent pendingEvent;

    protected UserSafetyStatus() { }

    public static UserSafetyStatus clean(UUID userId) {
        UserSafetyStatus s = new UserSafetyStatus();
        s.userId = userId.toString();
        return s;
    }

    /** @return true only the first time, so callers can react once. */
    public boolean putUnderReview(String reason, PendingEvent event) {
        if (underReview) return false;
        this.underReview = true;
        this.reviewReason = reason;
        this.underReviewSince = Instant.now();
        this.pendingEvent = event;
        return true;
    }

    public void clearReview(PendingEvent event) {
        this.underReview = false;
        this.reviewReason = null;
        this.underReviewSince = null;
        this.pendingEvent = event;
    }

    @Override public String outboxId() { return userId; }
    @Override public PendingEvent getPendingEvent() { return pendingEvent; }

    public UUID getUserId() { return UUID.fromString(userId); }
    public boolean isUnderReview() { return underReview; }
    public String getReviewReason() { return reviewReason; }
    public Instant getUnderReviewSince() { return underReviewSince; }
}
