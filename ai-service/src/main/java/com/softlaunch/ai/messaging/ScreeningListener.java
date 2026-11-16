package com.softlaunch.ai.messaging;

import com.softlaunch.ai.scam.MessageScreeningEvent;
import com.softlaunch.ai.scam.ScamScreeningService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScreeningListener {

    private final ScamScreeningService screeningService;
    private final JsonMapper json;
    private final Validator validator;

    public ScreeningListener(ScamScreeningService screeningService, JsonMapper json, Validator validator) {
        this.screeningService = screeningService;
        this.json = json;
        this.validator = validator;
    }

    @KafkaListener(topics = "message.screening")
    public void onMessage(String payload) {
        MessageScreeningEvent event = json.readValue(payload, MessageScreeningEvent.class);
        Set<ConstraintViolation<MessageScreeningEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            // the error text lists field names only, never the payload (it contains the message text)
            throw new IllegalArgumentException("Invalid MessageScreeningEvent: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).collect(Collectors.joining(", ")));
        }
        screeningService.screen(event);
    }
}
