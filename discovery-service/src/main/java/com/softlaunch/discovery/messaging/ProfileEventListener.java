package com.softlaunch.discovery.messaging;

import com.softlaunch.discovery.event.ProfileUpdatedEvent;
import com.softlaunch.discovery.service.FeedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProfileEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProfileEventListener.class);

    private final FeedCache feedCache;
    private final JsonMapper jsonMapper;

    public ProfileEventListener(FeedCache feedCache, JsonMapper jsonMapper) {
        this.feedCache = feedCache;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "profile.updated")
    public void onProfileUpdated(String payload) {
        try {
            ProfileUpdatedEvent event = jsonMapper.readValue(payload, ProfileUpdatedEvent.class);
            if (event.userId() != null) {
                feedCache.evict(event.userId());
            }
        } catch (JacksonException e) {
            log.warn("Ignoring unreadable profile.updated event: {}", payload);
        }
    }
}