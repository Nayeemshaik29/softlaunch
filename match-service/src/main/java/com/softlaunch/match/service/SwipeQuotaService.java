package com.softlaunch.match.service;

import com.softlaunch.match.dto.SwipeQuota;
import com.softlaunch.match.exception.SwipeLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Daily swipe allowance ("energy" in the app). One Redis counter per user per day: swipes:<user>:<date>.
 * The day follows the users' local midnight (softlaunch.swipes.reset-zone, India by default),
 * not UTC, which would reset at 5:30 AM in India.
 */
@Service
public class SwipeQuotaService {

    private final StringRedisTemplate redis;
    private final int dailyLimit;
    private final ZoneId zone;
    private final Clock clock;

    @Autowired   // two constructors: tell Spring which one (the other is for tests with a fixed clock)
    public SwipeQuotaService(StringRedisTemplate redis,
                             @Value("${softlaunch.swipes.daily-limit:50}") int dailyLimit,
                             @Value("${softlaunch.swipes.reset-zone:Asia/Kolkata}") ZoneId zone) {
        this(redis, dailyLimit, zone, Clock.systemUTC());
    }

    SwipeQuotaService(StringRedisTemplate redis, int dailyLimit, ZoneId zone, Clock clock) {
        this.redis = redis;
        this.dailyLimit = dailyLimit;
        this.zone = zone;
        this.clock = clock;
    }

    public void consume(UUID userId) {
        String key = key(userId);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofDays(2));
        }
        if (count != null && count > dailyLimit) {
            throw new SwipeLimitExceededException(dailyLimit, resetsAt());
        }
    }

    /** For the app's energy meter. Reading never uses up a swipe. */
    public SwipeQuota status(UUID userId) {
        String value = redis.opsForValue().get(key(userId));
        int used = value == null ? 0 : (int) Math.min(Long.parseLong(value), dailyLimit);
        return new SwipeQuota(dailyLimit, dailyLimit - used, resetsAt());
    }

    /** Next local midnight, as an exact instant (the app counts down to it). */
    Instant resetsAt() {
        return today().plusDays(1).atStartOfDay(zone).toInstant();
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    private String key(UUID userId) {
        return "swipes:" + userId + ":" + today();
    }
}
