package com.softlaunch.notification.messaging;

import com.softlaunch.notification.event.MatchCreatedEvent;
import com.softlaunch.notification.event.MatchRemovedEvent;
import com.softlaunch.notification.exception.InvalidEventException;
import com.softlaunch.notification.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MatchEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchEventListener.class);

    private final NotificationService notificationService;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public MatchEventListener(NotificationService notificationService, JsonMapper jsonMapper, Validator validator) {
        this.notificationService = notificationService;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @KafkaListener(topics = "match.created")
    public void onMatchCreated(String payload) {
        log.info("Received match.created: {}", payload);
        notificationService.handleMatchCreated(parseAndValidate(payload, MatchCreatedEvent.class));
    }

    @KafkaListener(topics = "match.removed")
    public void onMatchRemoved(String payload) {
        log.info("Received match.removed: {}", payload);
        notificationService.handleMatchRemoved(parseAndValidate(payload, MatchRemovedEvent.class));
    }

    private <T> T parseAndValidate(String payload, Class<T> type) {
        T event = jsonMapper.readValue(payload, type);
        Set<ConstraintViolation<T>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            String reason = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new InvalidEventException("Invalid " + type.getSimpleName() + ": " + reason);
        }
        return event;
    }
}