package com.softlaunch.user.photo;

import com.softlaunch.user.event.ProfileUpdatedEvent;
import com.softlaunch.user.model.Profile;
import com.softlaunch.user.model.ProfileLimits;
import com.softlaunch.user.repository.ProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Photo uploads in 3 steps, so photo bytes never pass through our servers on the way IN:
 *   1. uploadLink: we check type/size/count and hand out a signed PUT link (valid a few minutes)
 *   2. the phone uploads straight to S3
 *   3. confirm: we read it back, process it (real image? strip metadata, resize), store the result, add it to the profile
 * Logs ids only, never image data.
 */
@Service
public class PhotoService implements PhotoLinks {

    private static final Logger log = LoggerFactory.getLogger(PhotoService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png");
    // decoding a 12 MP photo needs ~50 MB of memory: at most 2 at a time keeps the 512 MB container safe
    private final Semaphore processing = new Semaphore(2);

    private final PhotoStorage storage;
    private final PhotoUploadRepository uploads;
    private final ProfileRepository profiles;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final PhotoProperties props;
    private final MeterRegistry metrics;

    public PhotoService(PhotoStorage storage, PhotoUploadRepository uploads, ProfileRepository profiles,
                        ApplicationEventPublisher events, TransactionTemplate tx, PhotoProperties props, MeterRegistry metrics) {
        this.storage = storage;
        this.uploads = uploads;
        this.profiles = profiles;
        this.events = events;
        this.tx = tx;
        this.props = props;
        this.metrics = metrics;
    }

    public record UploadTicket(UUID photoId, String uploadUrl, String method, Map<String, String> headers, Instant expiresAt) {
    }

    public record PhotoView(UUID photoId, String url) {
    }

    // ---------- step 1 ----------

    public UploadTicket uploadLink(UUID userId, String contentType, long sizeBytes) {
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw reject("bad_type", HttpStatus.BAD_REQUEST, "Only JPEG and PNG photos are supported");
        }
        if (sizeBytes <= 0 || sizeBytes > props.maxUploadBytes()) {
            throw reject("too_big", HttpStatus.BAD_REQUEST, "Photo must be at most " + props.maxUploadBytes() / 1_048_576 + " MB");
        }
        Profile profile = profiles.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Create your profile first"));
        long pending = uploads.countByUserIdAndStatusAndExpiresAtAfter(userId, PhotoUpload.Status.PENDING, Instant.now());
        if (profile.getPhotoKeys().size() + pending >= ProfileLimits.MAX_PHOTOS) {
            throw reject("limit", HttpStatus.CONFLICT, "You can have at most " + ProfileLimits.MAX_PHOTOS + " photos");
        }

        Instant linkExpiry = Instant.now().plus(props.uploadLinkTtl());
        PhotoUpload upload = uploads.save(new PhotoUpload(userId, contentType, sizeBytes, Instant.now().plus(props.pendingTtl())));
        String url = storage.uploadLink(upload.pendingKey(), contentType, sizeBytes, props.uploadLinkTtl()).toString();
        return new UploadTicket(upload.getId(), url, "PUT", Map.of("Content-Type", contentType), linkExpiry);
    }

    // ---------- step 3 ----------

    public PhotoView confirm(UUID userId, UUID photoId) {
        PhotoUpload upload = uploads.findByIdAndUserId(photoId, userId)   // someone else's id -> 404, same as "doesn't exist"
                .filter(u -> u.getStatus() == PhotoUpload.Status.PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload not found"));

        HeadObjectResponse head = storage.head(upload.pendingKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Upload the photo first"));
        if (head.contentLength() > props.maxUploadBytes()) {   // S3 enforced the signed size; this is a second lock
            storage.delete(upload.pendingKey());
            throw reject("too_big", HttpStatus.BAD_REQUEST, "Photo is too big");
        }

        ImageProcessor.Processed processed = process(upload);
        storage.writeJpeg(upload.photoKey(), processed.jpeg());
        storage.delete(upload.pendingKey());   // the raw original (with its metadata) is gone for good

        tx.executeWithoutResult(status -> {
            Profile profile = profiles.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Create your profile first"));
            if (profile.getPhotoKeys().size() >= ProfileLimits.MAX_PHOTOS) {
                throw reject("limit", HttpStatus.CONFLICT, "You can have at most " + ProfileLimits.MAX_PHOTOS + " photos");
            }
            profile.getPhotoKeys().add(upload.photoKey());
            upload.markDone();
            uploads.save(upload);
            events.publishEvent(ProfileUpdatedEvent.of(userId));   // feeds refresh (AFTER_COMMIT)
        });
        metrics.counter("softlaunch.photos", "result", "uploaded").increment();
        log.info("Photo {} of user {} stored ({}x{}, {} KB)", photoId, userId, processed.width(), processed.height(),
                processed.jpeg().length / 1024);
        return new PhotoView(photoId, storage.viewLink(upload.photoKey(), props.viewLinkTtl()).toString());
    }

    private ImageProcessor.Processed process(PhotoUpload upload) {
        try {
            if (!processing.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Busy, please try again");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Busy, please try again");
        }
        try {
            return ImageProcessor.process(storage.read(upload.pendingKey()), props.maxPixels(), props.maxDimension());
        } catch (ImageProcessor.InvalidImageException e) {
            storage.delete(upload.pendingKey());
            throw reject("not_an_image", HttpStatus.BAD_REQUEST, e.getMessage());
        } finally {
            processing.release();
        }
    }

    // ---------- delete ----------

    public void delete(UUID userId, UUID photoId) {
        String key = PhotoKeys.of(userId, photoId);   // built from the caller's own id: can't touch other users' photos
        Boolean removed = tx.execute(status -> profiles.findById(userId).map(profile -> {
            boolean found = profile.getPhotoKeys().remove(key);
            if (found) events.publishEvent(ProfileUpdatedEvent.of(userId));
            return found;
        }).orElse(false));
        if (!Boolean.TRUE.equals(removed)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found");
        }
        storage.delete(key);
    }

    /** New order of the user's photos (first = main photo). Must list exactly the photos they have. */
    public void reorder(UUID userId, List<UUID> photoIds) {
        List<String> wanted = photoIds.stream().map(id -> PhotoKeys.of(userId, id)).toList();
        boolean ok = Boolean.TRUE.equals(tx.execute(status -> profiles.findById(userId).map(profile -> {
            List<String> current = profile.getPhotoKeys();
            if (wanted.size() != current.size() || !new java.util.HashSet<>(wanted).equals(new java.util.HashSet<>(current))) {
                return false;
            }
            current.clear();
            current.addAll(wanted);
            events.publishEvent(ProfileUpdatedEvent.of(userId));
            return true;
        }).orElse(false)));
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "List each of your photos exactly once");
        }
    }

    /** For account deletion: every stored file of this user. */
    public int deleteAllOf(UUID userId) {
        return storage.deleteAllOf(userId.toString());
    }

    // ---------- links for responses (PhotoLinks) ----------

    /** Signing is pure math on this machine (no call to S3), so it's cheap even for a whole feed. */
    @Override
    public List<String> of(List<String> photoKeys) {
        return photoKeys.stream().map(key -> storage.viewLink(key, props.viewLinkTtl()).toString()).toList();
    }

    // ---------- cleanup ----------

    /** Uploads started but never confirmed: delete the raw file and the row. */
    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT1M")
    public void cleanupAbandoned() {
        List<PhotoUpload> expired = uploads.findTop200ByStatusAndExpiresAtBefore(PhotoUpload.Status.PENDING, Instant.now());
        for (PhotoUpload upload : expired) {
            try {
                storage.delete(upload.pendingKey());
                uploads.delete(upload);
            } catch (RuntimeException e) {
                log.warn("Could not clean up upload {}: {}", upload.getId(), e.getClass().getSimpleName());
            }
        }
        if (!expired.isEmpty()) {
            log.info("Cleaned up {} abandoned photo uploads", expired.size());
            metrics.counter("softlaunch.photos", "result", "abandoned").increment(expired.size());
        }
    }

    private ResponseStatusException reject(String reason, HttpStatus status, String message) {
        metrics.counter("softlaunch.photos", "result", "rejected_" + reason).increment();
        return new ResponseStatusException(status, message);
    }
}
