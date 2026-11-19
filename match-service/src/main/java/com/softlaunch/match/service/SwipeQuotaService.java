package com.softlaunch.match.service;

import com.softlaunch.match.dto.SwipeQuota;
import com.softlaunch.match.exception.SuperLikeLimitExceededException;
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
    private final Limits limits;
    private final ZoneId zone;
    private final Clock clock;
    private final PlusChecker plus;

    /** Free vs Plus allowances per day (softlaunch.swipes.*). */
    public record Limits(int swipes, int plusSwipes, int superLikes, int plusSuperLikes) {
    }

    @Autowired   // two constructors: tell Spring which one (the other is for tests with a fixed clock)
    public SwipeQuotaService(StringRedisTemplate redis, PlusChecker plus,
                             @Value("${softlaunch.swipes.daily-limit:50}") int dailyLimit,
                             @Value("${softlaunch.swipes.plus-daily-limit:100}") int plusDailyLimit,
                             @Value("${softlaunch.swipes.super-likes:1}") int superLikes,
                             @Value("${softlaunch.swipes.plus-super-likes:5}") int plusSuperLikes,
                             @Value("${softlaunch.swipes.reset-zone:Asia/Kolkata}") ZoneId zone) {
        this(redis, plus, new Limits(dailyLimit, plusDailyLimit, superLikes, plusSuperLikes), zone, Clock.systemUTC());
    }

    SwipeQuotaService(StringRedisTemplate redis, PlusChecker plus, Limits limits, ZoneId zone, Clock clock) {
        this.redis = redis;
        this.plus = plus;
        this.limits = limits;
        this.zone = zone;
        this.clock = clock;
    }

    public void consume(UUID userId) {
        int limit = plus.isPlus(userId) ? limits.plusSwipes() : limits.swipes();
        if (increment(key(userId)) > limit) {
            throw new SwipeLimitExceededException(limit, resetsAt());
        }
    }

    /** Super likes have their own (smaller) daily allowance on top of the swipe one. */
    public void consumeSuperLike(UUID userId) {
        boolean isPlus = plus.isPlus(userId);
        int limit = isPlus ? limits.plusSuperLikes() : limits.superLikes();
        if (increment(superKey(userId)) > limit) {
            throw new SuperLikeLimitExceededException(limit, resetsAt(), isPlus);
        }
    }

    /** Give a super like back (the swipe itself was refused afterwards). */
    public void refundSuperLike(UUID userId) {
        redis.opsForValue().decrement(superKey(userId));
    }

    private long increment(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofDays(2));
        }
        return count == null ? 0 : count;
    }

    /** For the app's energy meter. Reading never uses up a swipe. */
    public SwipeQuota status(UUID userId) {
        boolean isPlus = plus.isPlus(userId);
        int limit = isPlus ? limits.plusSwipes() : limits.swipes();
        int superLimit = isPlus ? limits.plusSuperLikes() : limits.superLikes();
        return new SwipeQuota(limit, limit - used(key(userId), limit), resetsAt(),
                superLimit, superLimit - used(superKey(userId), superLimit), isPlus);
    }

    private int used(String key, int cap) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : (int) Math.max(0, Math.min(Long.parseLong(value), cap));
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

    private String superKey(UUID userId) {
        return "superlikes:" + userId + ":" + today();
    }
}
