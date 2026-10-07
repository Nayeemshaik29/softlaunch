package com.softlaunch.notification.messaging;

import com.softlaunch.notification.event.MatchCreatedEvent;
import com.softlaunch.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MatchEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchEventListener.class);

    private final NotificationService notificationService;
    private final JsonMapper jsonMapper;

    public MatchEventListener(NotificationService notificationService, JsonMapper jsonMapper) {
        this.notificationService = notificationService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "match.created")
    public void onMatchCreated(String payload) {
        log.info("Received match.created: {}", payload);
        MatchCreatedEvent event = jsonMapper.readValue(payload, MatchCreatedEvent.class);
        notificationService.handleMatchCreated(event);
    }
}