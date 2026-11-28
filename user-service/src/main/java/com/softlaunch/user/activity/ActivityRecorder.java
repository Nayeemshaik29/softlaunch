package com.softlaunch.user.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Activity counters for the admin dashboard, in Redis (shared with the other services, same key names).
 * COUNTS ONLY: never message text, call content, locations or anything a user typed.
 * Best effort: a Redis hiccup must never break a login, so every write swallows its error.
 *
 * Keys: act:user:{id} (hash of counters) · act:logins:{day} (hash id→count) · act:logins:{day}:total ·
 *       act:feed (last 200 events) · act:day:{day} (set, gateway) · act:recent (zset, gateway) · act:reported (zset, safety)
 */
@Component
public class ActivityRecorder {

    public static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final Duration DAY_KEYS_TTL = Duration.ofDays(40);
    private static final Logger log = LoggerFactory.getLogger(ActivityRecorder.class);

    private final StringRedisTemplate redis;

    public ActivityRecorder(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public static String today() {
        return LocalDate.now(IST).toString();
    }

    public void count(UUID userId, String field, long by) {
        try {
            redis.opsForHash().increment("act:user:" + userId, field, by);
        } catch (RuntimeException e) {
            log.debug("activity count skipped: {}", e.getClass().getSimpleName());
        }
    }

    /** kind is a fixed code ("login", "signup_google"...), never user input. */
    public void feed(UUID userId, String kind) {
        try {
            redis.opsForList().leftPush("act:feed", "{\"u\":\"" + userId + "\",\"k\":\"" + kind + "\",\"at\":\"" + Instant.now() + "\"}");
            redis.opsForList().trim("act:feed", 0, 199);
        } catch (RuntimeException e) {
            log.debug("activity feed skipped: {}", e.getClass().getSimpleName());
        }
    }

    public void login(UUID userId) {
        try {
            String day = today();
            redis.opsForHash().increment("act:logins:" + day, userId.toString(), 1);
            redis.expire("act:logins:" + day, DAY_KEYS_TTL);
            redis.opsForValue().increment("act:logins:" + day + ":total");
            redis.expire("act:logins:" + day + ":total", DAY_KEYS_TTL);
        } catch (RuntimeException e) {
            log.debug("activity login skipped: {}", e.getClass().getSimpleName());
        }
        count(userId, "logins", 1);
        feed(userId, "login");
    }

    /** Account deleted: its counters go too. */
    public void forget(UUID userId) {
        try {
            redis.delete("act:user:" + userId);
            redis.opsForZSet().remove("act:recent", userId.toString());
            redis.opsForZSet().remove("act:reported", userId.toString());
        } catch (RuntimeException e) {
            log.debug("activity forget skipped: {}", e.getClass().getSimpleName());
        }
    }
}
