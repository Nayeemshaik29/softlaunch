package com.softlaunch.user.messaging;

import com.softlaunch.user.event.UserVerifiedEvent;
import com.softlaunch.user.exception.InvalidEventException;
import com.softlaunch.user.service.UserVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class VerificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(VerificationEventListener.class);

    private final UserVerificationService userVerificationService;
    private final JsonMapper jsonMapper;

    public VerificationEventListener(UserVerificationService userVerificationService, JsonMapper jsonMapper) {
        this.userVerificationService = userVerificationService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "user.verified")
    public void onUserVerified(String payload) {
        log.info("Received user.verified: {}", payload);
        UserVerifiedEvent event = jsonMapper.readValue(payload, UserVerifiedEvent.class);
        if (event.eventId() == null || event.userId() == null || event.verifiedAt() == null) {
            throw new InvalidEventException("Invalid UserVerifiedEvent: missing eventId, userId or verifiedAt");
        }
        userVerificationService.markVerified(event);
    }
}
