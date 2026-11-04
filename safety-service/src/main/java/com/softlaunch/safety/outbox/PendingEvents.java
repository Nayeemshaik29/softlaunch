package com.softlaunch.safety.outbox;

import com.softlaunch.safety.config.KafkaTopicConfig;
import com.softlaunch.safety.event.BlockChangedEvent;
import com.softlaunch.safety.event.ReviewStatusEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/** Builds PendingEvents (topic + key + JSON) so the domain code never touches Kafka. */
@Component
public class PendingEvents {

    private final JsonMapper jsonMapper;

    public PendingEvents(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public PendingEvent of(BlockChangedEvent e) {
        return new PendingEvent(e.eventId().toString(), KafkaTopicConfig.BLOCK_CHANGED, e.pairKey(),
                jsonMapper.writeValueAsString(e), Instant.now());
    }

    public PendingEvent of(ReviewStatusEvent e) {
        return new PendingEvent(e.eventId().toString(), KafkaTopicConfig.REVIEW_STATUS, e.userId().toString(),
                jsonMapper.writeValueAsString(e), Instant.now());
    }
}
