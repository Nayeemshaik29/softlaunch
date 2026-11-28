package com.softlaunch.user.deletion;

import com.softlaunch.user.exception.InvalidEventException;
import com.softlaunch.user.photo.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * user-service listens to its OWN user.deleted event to remove the photos from S3.
 * Why not just delete them in the request? S3 can be down: here a failure is retried and, if it keeps
 * failing, lands in user.deleted.DLT where we can see it. Idempotent: deleting nothing is fine.
 */
@Component
public class PhotoCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(PhotoCleanupListener.class);

    private final PhotoService photos;
    private final JsonMapper json;
    private final com.softlaunch.user.privacy.PrivacyService privacy;
    private final com.softlaunch.user.billing.BillingService billing;
    private final com.softlaunch.user.activity.ActivityRecorder activity;

    public PhotoCleanupListener(PhotoService photos, JsonMapper json, com.softlaunch.user.privacy.PrivacyService privacy,
                                com.softlaunch.user.billing.BillingService billing,
                                com.softlaunch.user.activity.ActivityRecorder activity) {
        this.activity = activity;
        this.billing = billing;
        this.privacy = privacy;
        this.photos = photos;
        this.json = json;
    }

    @KafkaListener(topics = AccountDeletionService.TOPIC)
    public void onUserDeleted(String payload) {
        UserDeletedEvent event = json.readValue(payload, UserDeletedEvent.class);
        if (event.userId() == null) {
            throw new InvalidEventException("user.deleted without userId");
        }
        int removed = photos.deleteAllOf(event.userId());   // originals AND blurred copies (same folder)
        privacy.forget(event.userId());
        activity.forget(event.userId());  // admin dashboard counters
        billing.forget(event.userId());   // Plus ends; purchase records stay for tax/refunds (ids + amounts only)
        log.info("Deleted user {}: {} photo file(s) removed from storage", event.userId(), removed);
    }
}
