package com.softlaunch.match.service;

import com.softlaunch.match.exception.SwipeLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class SwipeQuotaService {

    private final StringRedisTemplate redis;
    private final int dailyLimit;

    public SwipeQuotaService(StringRedisTemplate redis,
                             @Value("${softlaunch.swipes.daily-limit:50}") int dailyLimit) {
        this.redis = redis;
        this.dailyLimit = dailyLimit;
    }

    public void consume(UUID userId) {
        String key = "swipes:" + userId + ":" + LocalDate.now(ZoneOffset.UTC);

        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofDays(2));
        }

        if (count != null && count > dailyLimit) {
            throw new SwipeLimitExceededException(dailyLimit);
        }
    }
}