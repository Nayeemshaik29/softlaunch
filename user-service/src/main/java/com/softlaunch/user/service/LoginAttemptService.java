package com.softlaunch.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Account lockout: counts wrong passwords PER EMAIL in Redis, so it also stops a botnet that uses
 * a new IP for every guess (the gateway's per-IP limit can't see that).
 *
 * - Counted for every email, even ones with no account: a locked fake email and a locked real one
 *   look identical, so the lock can't be used to discover who is registered.
 * - The Redis key holds a SHA-256 of the email, never the email itself.
 * - Short window (15 min): anyone can lock Priya out on purpose by typing her email, so the lock must hurt little.
 * - If Redis is down we allow the login (fail open): a Redis outage must not lock out every user.
 *   The gateway rate limit and the failed-login alert still protect us meanwhile.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final StringRedisTemplate redis;
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginAttemptService(StringRedisTemplate redis,
                               @Value("${softlaunch.login-lockout.max-failures:5}") int maxFailures,
                               @Value("${softlaunch.login-lockout.lock-duration:15m}") Duration lockDuration) {
        this.redis = redis;
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
    }

    public boolean isLocked(String email) {
        try {
            String count = redis.opsForValue().get(key(email));
            return count != null && Long.parseLong(count) >= maxFailures;
        } catch (DataAccessException e) {
            log.warn("Login lockout check skipped, Redis unavailable: {}", e.getMessage());
            return false;
        }
    }

    public void recordFailure(String email) {
        try {
            String key = key(email);
            Long failures = redis.opsForValue().increment(key);
            if (failures != null && failures == 1) {
                redis.expire(key, lockDuration);   // window starts at the first failure
            }
        } catch (DataAccessException e) {
            log.warn("Could not record failed login, Redis unavailable: {}", e.getMessage());
        }
    }

    public void recordSuccess(String email) {
        try {
            redis.delete(key(email));
        } catch (DataAccessException e) {
            log.warn("Could not reset failed logins, Redis unavailable: {}", e.getMessage());
        }
    }

    private static String key(String email) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8));
            return "login:fail:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
