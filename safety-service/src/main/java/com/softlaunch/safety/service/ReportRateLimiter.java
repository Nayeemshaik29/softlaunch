package com.softlaunch.safety.service;

import com.softlaunch.safety.config.SafetyProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/** Same idea as the swipe quota in match-service: one Redis counter per user per day. */
@Component
public class ReportRateLimiter {

    private final StringRedisTemplate redis;
    private final SafetyProperties props;

    public ReportRateLimiter(StringRedisTemplate redis, SafetyProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public void check(UUID reporterId) {
        String key = "reports:" + reporterId + ":" + LocalDate.now(ZoneOffset.UTC);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofDays(1));
        }
        if (count != null && count > props.dailyReportLimit()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You've sent a lot of reports today. Please try again tomorrow.");
        }
    }
}
