package com.softlaunch.discovery.service;

import com.softlaunch.discovery.dto.FeedCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FeedCache {

    private static final Logger log = LoggerFactory.getLogger(FeedCache.class);
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final TypeReference<List<FeedCard>> FEED_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;

    public FeedCache(StringRedisTemplate redis, JsonMapper jsonMapper) {
        this.redis = redis;
        this.jsonMapper = jsonMapper;
    }

    public Optional<List<FeedCard>> get(UUID me, int radiusKm) {
        try {
            HashOperations<String, String, String> hash = redis.opsForHash();
            String json = hash.get(key(me), String.valueOf(radiusKm));
            if (json == null) {
                log.info("Feed cache MISS for {} ({} km)", me, radiusKm);
                return Optional.empty();
            }
            log.info("Feed cache HIT for {} ({} km)", me, radiusKm);
            return Optional.of(jsonMapper.readValue(json, FEED_TYPE));
        } catch (RuntimeException e) {
            log.warn("Feed cache read failed for {}, rebuilding", me, e);
            return Optional.empty();
        }
    }

    public void put(UUID me, int radiusKm, List<FeedCard> feed) {
        try {
            HashOperations<String, String, String> hash = redis.opsForHash();
            hash.put(key(me), String.valueOf(radiusKm), jsonMapper.writeValueAsString(feed));
            redis.expire(key(me), TTL);
        } catch (RuntimeException e) {
            log.warn("Feed cache write failed for {}", me, e);
        }
    }

    public void evict(UUID me) {
        redis.delete(key(me));
        log.info("Feed cache EVICTED for {}", me);
    }

    private String key(UUID me) {
        return "feed:" + me;
    }
}