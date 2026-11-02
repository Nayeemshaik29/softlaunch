package com.softlaunch.verification.service;

import com.softlaunch.verification.config.KafkaTopicConfig;
import com.softlaunch.verification.event.UserVerifiedEvent;
import com.softlaunch.verification.model.OutboxEvent;
import com.softlaunch.verification.model.Verification;
import com.softlaunch.verification.repository.OutboxRepository;
import com.softlaunch.verification.repository.VerificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Separate bean on purpose: @Transactional only works when called from ANOTHER bean
 * (a call to this.method() inside the same class skips Spring's proxy = no transaction).
 */
@Component
public class VerificationWriter {

    private final VerificationRepository verificationRepository;
    private final OutboxRepository outboxRepository;
    private final JsonMapper jsonMapper;

    public VerificationWriter(VerificationRepository verificationRepository, OutboxRepository outboxRepository,
                              JsonMapper jsonMapper) {
        this.verificationRepository = verificationRepository;
        this.outboxRepository = outboxRepository;
        this.jsonMapper = jsonMapper;
    }

    /** Both rows are saved, or neither. */
    @Transactional
    public void saveVerified(Verification verification, UserVerifiedEvent event) {
        verificationRepository.saveAndFlush(verification);   // flush now so a unique-index clash fails here
        outboxRepository.save(OutboxEvent.of(KafkaTopicConfig.USER_VERIFIED,
                event.userId().toString(), jsonMapper.writeValueAsString(event)));
    }
}
