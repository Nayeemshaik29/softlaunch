package com.softlaunch.match.service;

import com.softlaunch.match.exception.SwipeLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SwipeQuotaServiceTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final UUID me = UUID.randomUUID();

    private SwipeQuotaService at(String utcInstant) {
        when(redis.opsForValue()).thenReturn(values);
        return new SwipeQuotaService(redis, 3, INDIA, Clock.fixed(Instant.parse(utcInstant), ZoneOffset.UTC));
    }

    @Test
    void dayFollowsIndianMidnightNotUtc() {
        // 20:00 UTC on 18 Nov = 01:30 on 19 Nov in India: already the NEW day there
        SwipeQuotaService quota = at("2026-11-18T20:00:00Z");
        when(values.increment("swipes:" + me + ":2026-11-19")).thenReturn(1L);
        quota.consume(me);
        verify(values).increment("swipes:" + me + ":2026-11-19");
        assertThat(quota.resetsAt()).isEqualTo(Instant.parse("2026-11-19T18:30:00Z"));   // next IST midnight
    }

    @Test
    void statusShowsWhatIsLeftWithoutUsingASwipe() {
        SwipeQuotaService quota = at("2026-11-18T06:00:00Z");
        when(values.get("swipes:" + me + ":2026-11-18")).thenReturn("2");
        var status = quota.status(me);
        assertThat(status.limit()).isEqualTo(3);
        assertThat(status.remaining()).isEqualTo(1);
        verify(values, never()).increment(anyString());
    }

    @Test
    void overTheLimitIsRefusedWithTheResetTime() {
        SwipeQuotaService quota = at("2026-11-18T06:00:00Z");
        when(values.increment(anyString())).thenReturn(4L);
        assertThatThrownBy(() -> quota.consume(me)).isInstanceOf(SwipeLimitExceededException.class)
                .extracting(e -> ((SwipeLimitExceededException) e).getResetsAt())
                .isEqualTo(Instant.parse("2026-11-18T18:30:00Z"));
    }

    @Test
    void remainingNeverGoesNegative() {
        SwipeQuotaService quota = at("2026-11-18T06:00:00Z");
        when(values.get(anyString())).thenReturn("9");
        assertThat(quota.status(me).remaining()).isZero();
    }
}
