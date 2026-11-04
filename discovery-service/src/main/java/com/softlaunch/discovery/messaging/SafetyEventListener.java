package com.softlaunch.discovery.messaging;

import com.softlaunch.discovery.event.BlockChangedEvent;
import com.softlaunch.discovery.event.ReviewStatusEvent;
import com.softlaunch.discovery.service.FeedCache;
import com.softlaunch.discovery.service.SafetyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * auto.offset.reset=earliest just for these listeners: the rest of discovery uses "latest" (cache hints),
 * but a missed block event would be a safety bug, so we must read every one of them.
 */
@Component
public class SafetyEventListener {

    private static final Logger log = LoggerFactory.getLogger(SafetyEventListener.class);

    private final SafetyFilter safetyFilter;
    private final FeedCache feedCache;
    private final JsonMapper jsonMapper;

    public SafetyEventListener(SafetyFilter safetyFilter, FeedCache feedCache, JsonMapper jsonMapper) {
        this.safetyFilter = safetyFilter;
        this.feedCache = feedCache;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "block.changed", properties = "auto.offset.reset=earliest")
    public void onBlockChanged(String payload) {
        try {
            BlockChangedEvent e = jsonMapper.readValue(payload, BlockChangedEvent.class);
            if (e.blockerId() == null || e.blockedId() == null) {
                log.warn("Ignoring block.changed without ids: {}", payload);
                return;
            }
            if (e.active()) {
                safetyFilter.block(e.blockerId(), e.blockedId());
            } else {
                safetyFilter.unblock(e.blockerId(), e.blockedId());
            }
            feedCache.evict(e.blockerId());
            feedCache.evict(e.blockedId());
            log.info("Block {} -> {} active={} applied to feeds", e.blockerId(), e.blockedId(), e.active());
        } catch (JacksonException ex) {
            log.warn("Ignoring unreadable block.changed event: {}", payload);
        }
    }

    @KafkaListener(topics = "user.review_status", properties = "auto.offset.reset=earliest")
    public void onReviewStatus(String payload) {
        try {
            ReviewStatusEvent e = jsonMapper.readValue(payload, ReviewStatusEvent.class);
            if (e.userId() == null) {
                log.warn("Ignoring user.review_status without userId: {}", payload);
                return;
            }
            safetyFilter.setUnderReview(e.userId(), e.underReview());
            log.info("User {} underReview={} applied to feeds", e.userId(), e.underReview());
        } catch (JacksonException ex) {
            log.warn("Ignoring unreadable user.review_status event: {}", payload);
        }
    }
}
