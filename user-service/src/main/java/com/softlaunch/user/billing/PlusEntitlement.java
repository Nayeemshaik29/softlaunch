package com.softlaunch.user.billing;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * plus:<userId> in Redis, expiring exactly when the pass ends. match-service reads it for the perks
 * (100 swipes, 5 super likes, rewind, who liked you). The database stays the source of truth.
 */
@Component
public class PlusEntitlement {

    public static final String PREFIX = "plus:";
    private final StringRedisTemplate redis;

    public PlusEntitlement(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void activeUntil(UUID userId, Instant until) {
        Duration left = Duration.between(Instant.now(), until);
        if (left.isNegative() || left.isZero()) redis.delete(PREFIX + userId);
        else redis.opsForValue().set(PREFIX + userId, until.toString(), left);
    }

    public void forget(UUID userId) {
        redis.delete(PREFIX + userId);
    }
}
