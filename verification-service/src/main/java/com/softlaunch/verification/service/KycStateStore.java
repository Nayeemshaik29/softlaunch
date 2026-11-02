package com.softlaunch.verification.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class KycStateStore {

    private static final Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redis;

    public KycStateStore(StringRedisTemplate redis) { this.redis = redis; }

    public record PendingKyc(UUID userId, String codeVerifier) { }

    public void save(String state, UUID userId, String codeVerifier) {
        redis.opsForValue().set(key(state), userId + "|" + codeVerifier, TTL);
    }

    /** One-time use: read AND delete in one step, so a replayed callback finds nothing. */
    public Optional<PendingKyc> consume(String state) {
        String value = redis.opsForValue().getAndDelete(key(state));
        if (value == null) return Optional.empty();
        String[] parts = value.split("\\|", 2);
        return Optional.of(new PendingKyc(UUID.fromString(parts[0]), parts[1]));
    }

    private static String key(String state) { return "kyc:state:" + state; }
}
