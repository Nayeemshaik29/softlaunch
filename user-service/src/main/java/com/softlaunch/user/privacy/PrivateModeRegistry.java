package com.softlaunch.user.privacy;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Redis set of users in Private Mode, read on EVERY request by discovery (feed) and chat (online / last seen),
 * so switching it on takes effect at once, without waiting for caches. The database stays the source of truth.
 */
@Component
public class PrivateModeRegistry {

    public static final String KEY = "privacy:private_mode";
    /** Users with veiled photos: discovery strips real photo links from feeds that were cached before the switch. */
    public static final String VEILED_KEY = "privacy:veiled";

    private final StringRedisTemplate redis;

    public PrivateModeRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void set(UUID userId, boolean on) {
        toggle(KEY, userId, on);
    }

    public void setVeiled(UUID userId, boolean on) {
        toggle(VEILED_KEY, userId, on);
    }

    private void toggle(String key, UUID userId, boolean on) {
        if (on) redis.opsForSet().add(key, userId.toString());
        else redis.opsForSet().remove(key, userId.toString());
    }
}
