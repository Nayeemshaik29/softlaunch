package com.softlaunch.discovery.messaging;

import com.softlaunch.discovery.event.SwipeRecordedEvent;
import com.softlaunch.discovery.service.FeedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SwipeEventListener {

    private static final Logger log = LoggerFactory.getLogger(SwipeEventListener.class);

    private final FeedCache feedCache;
    private final JsonMapper jsonMapper;

    public SwipeEventListener(FeedCache feedCache, JsonMapper jsonMapper) {
        this.feedCache = feedCache;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "swipe.recorded")
    public void onSwipeRecorded(String payload) {
        try {
            SwipeRecordedEvent event = jsonMapper.readValue(payload, SwipeRecordedEvent.class);
            if (event.swiperId() != null) {
                feedCache.evict(event.swiperId());
            }
        } catch (JacksonException e) {
            log.warn("Ignoring unreadable swipe.recorded event: {}", payload);
        }
    }
}