package com.softlaunch.chat.service;

import com.softlaunch.chat.dto.PresenceResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class PresenceService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void connected(String userId, String sessionId) {
        redis.opsForSet().add(onlineKey(userId), sessionId);
        redis.expire(onlineKey(userId), ONLINE_TTL);
    }

    public void heartbeat(String userId) {
        redis.expire(onlineKey(userId), ONLINE_TTL);
    }

    public boolean disconnected(String userId, String sessionId) {
        redis.opsForSet().remove(onlineKey(userId), sessionId);
        Long remaining = redis.opsForSet().size(onlineKey(userId));
        boolean nowOffline = remaining == null || remaining == 0;
        if (nowOffline) {
            redis.opsForValue().set(lastSeenKey(userId), Instant.now().toString());
        }
        return nowOffline;
    }

    public PresenceResponse presenceOf(String userId) {
        boolean online = Boolean.TRUE.equals(redis.hasKey(onlineKey(userId)));
        String lastSeen = redis.opsForValue().get(lastSeenKey(userId));
        return new PresenceResponse(userId, online, lastSeen == null ? null : Instant.parse(lastSeen));
    }

    private String onlineKey(String userId) {
        return "presence:" + userId;
    }

    private String lastSeenKey(String userId) {
        return "lastseen:" + userId;
    }
}