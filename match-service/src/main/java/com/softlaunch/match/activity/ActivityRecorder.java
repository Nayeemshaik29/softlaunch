package com.softlaunch.match.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Activity counters for the admin dashboard (Redis; same keys as user-service's ActivityRecorder).
 * COUNTS ONLY, never content. Best effort: errors are swallowed so a Redis hiccup never breaks the real action.
 */
@Component
public class ActivityRecorder {

    private static final Logger log = LoggerFactory.getLogger(ActivityRecorder.class);
    private final StringRedisTemplate redis;

    public ActivityRecorder(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void count(UUID userId, String field, long by) {
        try {
            redis.opsForHash().increment("act:user:" + userId, field, by);
        } catch (RuntimeException e) {
            log.debug("activity count skipped: {}", e.getClass().getSimpleName());
        }
    }

    /** kind is a fixed code ("match", "call_video"...), never user input. */
    public void feed(UUID userId, String kind) {
        try {
            redis.opsForList().leftPush("act:feed", "{\"u\":\"" + userId + "\",\"k\":\"" + kind + "\",\"at\":\"" + Instant.now() + "\"}");
            redis.opsForList().trim("act:feed", 0, 199);
        } catch (RuntimeException e) {
            log.debug("activity feed skipped: {}", e.getClass().getSimpleName());
        }
    }

    /** safety-service: how often someone has been reported (admin "Reported" filter). */
    public void reported(UUID userId) {
        count(userId, "reports", 1);
        try {
            redis.opsForZSet().incrementScore("act:reported", userId.toString(), 1);
        } catch (RuntimeException e) {
            log.debug("activity reported skipped: {}", e.getClass().getSimpleName());
        }
        feed(userId, "reported");
    }
}
