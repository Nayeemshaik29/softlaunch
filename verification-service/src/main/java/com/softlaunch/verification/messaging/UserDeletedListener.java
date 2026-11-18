package com.softlaunch.verification.messaging;

import com.softlaunch.verification.repository.OutboxRepository;
import com.softlaunch.verification.repository.VerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * KYC data is the most sensitive thing we hold (name and date of birth from DigiLocker), so on account
 * deletion ALL of it goes, including the subject hash: that also frees the identity to verify a new
 * account later. Keeping banned people out would need a separate, explicit ban list (not built yet).
 * Idempotent: a second delivery deletes 0 rows.
 */
@Component
public class UserDeletedListener {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedListener.class);

    private final VerificationRepository verifications;
    private final OutboxRepository outbox;
    private final JsonMapper json;

    public UserDeletedListener(VerificationRepository verifications, OutboxRepository outbox, JsonMapper json) {
        this.verifications = verifications;
        this.outbox = outbox;
        this.json = json;
    }

    @KafkaListener(topics = "user.deleted")
    @Transactional
    public void onUserDeleted(String payload) {
        JsonNode value = json.readTree(payload).get("userId");
        if (value == null || value.isNull()) {
            log.warn("Ignoring user.deleted without userId");
            return;
        }
        UUID userId = UUID.fromString(value.asString());
        long rows = verifications.deleteByUserId(userId);
        long events = outbox.deleteByMessageKey(userId.toString());
        log.info("Deleted user {}: removed {} verification record(s), {} outbox event(s)", userId, rows, events);
    }
}
