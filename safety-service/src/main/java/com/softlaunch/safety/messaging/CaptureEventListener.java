package com.softlaunch.safety.messaging;

import com.softlaunch.safety.event.ScreenshotTakenEvent;
import com.softlaunch.safety.exception.InvalidEventException;
import com.softlaunch.safety.service.CaptureMonitor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CaptureEventListener {

    private final CaptureMonitor captureMonitor;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public CaptureEventListener(CaptureMonitor captureMonitor, JsonMapper jsonMapper, Validator validator) {
        this.captureMonitor = captureMonitor;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @KafkaListener(topics = "screenshot.taken")
    public void onScreenshotTaken(String payload) {
        ScreenshotTakenEvent event = jsonMapper.readValue(payload, ScreenshotTakenEvent.class);
        Set<ConstraintViolation<ScreenshotTakenEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new InvalidEventException("Invalid ScreenshotTakenEvent: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).collect(Collectors.joining(", ")));
        }
        captureMonitor.record(event);
    }
}
