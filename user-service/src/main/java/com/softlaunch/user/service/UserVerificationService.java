package com.softlaunch.user.service;

import com.softlaunch.user.event.ProfileUpdatedEvent;
import com.softlaunch.user.event.UserVerifiedEvent;
import com.softlaunch.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserVerificationService {

    private static final Logger log = LoggerFactory.getLogger(UserVerificationService.class);

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserVerificationService(UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Idempotent: a duplicate event (outbox = at-least-once) changes nothing. */
    @Transactional
    public void markVerified(UserVerifiedEvent event) {
        userRepository.findById(event.userId()).ifPresentOrElse(user -> {
            if (!user.markVerified(event.verifiedAt())) {
                log.info("User {} already verified, ignoring duplicate event {}", user.getId(), event.eventId());
                return;
            }
            // badge changed -> discovery must rebuild cached feeds that show this user
            eventPublisher.publishEvent(ProfileUpdatedEvent.of(user.getId()));
            log.info("User {} is now verified ✅", user.getId());
        }, () -> log.warn("user.verified for unknown user {} (account deleted?), skipping", event.userId()));
    }
}
