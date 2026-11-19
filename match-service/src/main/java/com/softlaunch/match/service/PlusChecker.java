package com.softlaunch.match.service;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Is this user on Plus right now? plus:<id> is written by user-service and expires when the pass ends. */
@Component
public class PlusChecker {

    private final StringRedisTemplate redis;

    public PlusChecker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean isPlus(UUID userId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey("plus:" + userId));
        } catch (DataAccessException e) {
            return false;   // Redis down: free limits apply (swipes need Redis anyway)
        }
    }
}
