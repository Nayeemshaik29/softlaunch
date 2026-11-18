package com.softlaunch.discovery.messaging;

import com.softlaunch.discovery.service.FeedCache;
import com.softlaunch.discovery.service.LocationService;
import com.softlaunch.discovery.service.SafetyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * earliest (like the safety listeners): a missed deletion would leave the person visible on the map.
 * Other people's cached feeds may still show their card for up to 2 minutes (cache TTL); swiping it
 * then fails cleanly, because match-service checks the user still exists.
 */
@Component
public class UserDeletedListener {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedListener.class);

    private final LocationService locations;
    private final SafetyFilter safetyFilter;
    private final FeedCache feedCache;
    private final JsonMapper json;

    public UserDeletedListener(LocationService locations, SafetyFilter safetyFilter, FeedCache feedCache, JsonMapper json) {
        this.locations = locations;
        this.safetyFilter = safetyFilter;
        this.feedCache = feedCache;
        this.json = json;
    }

    @KafkaListener(topics = "user.deleted", properties = "auto.offset.reset=earliest")
    public void onUserDeleted(String payload) {
        JsonNode value = json.readTree(payload).get("userId");
        if (value == null || value.isNull()) {
            log.warn("Ignoring user.deleted without userId");
            return;
        }
        UUID userId = UUID.fromString(value.asString());
        locations.forget(userId);
        safetyFilter.forget(userId);
        feedCache.evict(userId);
        log.info("Deleted user {}: removed from the map, block lists and feed cache", userId);
    }
}
