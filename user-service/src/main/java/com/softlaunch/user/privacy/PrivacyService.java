package com.softlaunch.user.privacy;

import com.softlaunch.user.event.ProfileUpdatedEvent;
import com.softlaunch.user.exception.ProfileNotFoundException;
import com.softlaunch.user.model.Profile;
import com.softlaunch.user.photo.PhotoService;
import com.softlaunch.user.repository.ProfileRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PrivacyService {

    private final ProfileRepository profiles;
    private final PhotoService photos;
    private final PrivateModeRegistry registry;
    private final ApplicationEventPublisher events;
    private final MeterRegistry metrics;

    public PrivacyService(ProfileRepository profiles, PhotoService photos, PrivateModeRegistry registry,
                          ApplicationEventPublisher events, MeterRegistry metrics) {
        this.profiles = profiles;
        this.photos = photos;
        this.registry = registry;
        this.events = events;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public PrivacySettings get(UUID userId) {
        Profile p = profiles.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
        return new PrivacySettings(p.isPrivateMode(), p.isVeiledPhotos());
    }

    @Transactional
    public PrivacySettings update(UUID userId, PrivacySettings change) {
        Profile p = profiles.findById(userId).orElseThrow(() -> new ProfileNotFoundException(userId));
        if (Boolean.TRUE.equals(change.veiledPhotos()) && !p.isVeiledPhotos()) {
            photos.ensureVeils(List.copyOf(p.getPhotoKeys()));   // blurred copies must exist BEFORE we serve them
        }
        if (change.privateMode() != null) {
            p.setPrivateMode(change.privateMode());
            registry.set(userId, change.privateMode());   // takes effect in feeds/chat immediately
            metrics.counter("softlaunch.privacy.changes", "setting", "private_mode", "on", change.privateMode().toString()).increment();
        }
        if (change.veiledPhotos() != null) {
            p.setVeiledPhotos(change.veiledPhotos());
            registry.setVeiled(userId, change.veiledPhotos());   // cached feeds lose the real links at once
            metrics.counter("softlaunch.privacy.changes", "setting", "veiled_photos", "on", change.veiledPhotos().toString()).increment();
        }
        p.touch();
        events.publishEvent(ProfileUpdatedEvent.of(userId));   // feeds rebuilt with the right photo links
        return new PrivacySettings(p.isPrivateMode(), p.isVeiledPhotos());
    }

    /** Account deleted: drop from the Private Mode set. */
    public void forget(UUID userId) {
        registry.set(userId, false);
        registry.setVeiled(userId, false);
    }
}
