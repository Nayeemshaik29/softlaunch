package com.softlaunch.safety.messaging;

import com.softlaunch.safety.event.ScamSuspectedEvent;
import com.softlaunch.safety.exception.InvalidEventException;
import com.softlaunch.safety.service.ScamMonitor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ScamEventListener {

    private final ScamMonitor scamMonitor;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public ScamEventListener(ScamMonitor scamMonitor, JsonMapper jsonMapper, Validator validator) {
        this.scamMonitor = scamMonitor;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @KafkaListener(topics = "scam.suspected")
    public void onScamSuspected(String payload) {
        ScamSuspectedEvent event = jsonMapper.readValue(payload, ScamSuspectedEvent.class);
        Set<ConstraintViolation<ScamSuspectedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new InvalidEventException("Invalid ScamSuspectedEvent: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).collect(Collectors.joining(", ")));
        }
        scamMonitor.record(event);
    }
}
