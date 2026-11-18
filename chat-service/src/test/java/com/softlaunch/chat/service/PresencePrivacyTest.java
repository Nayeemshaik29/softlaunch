package com.softlaunch.chat.service;

import com.softlaunch.chat.dto.PresenceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PresencePrivacyTest {

    @SuppressWarnings("unchecked")
    private final SetOperations<String, String> sets = mock(SetOperations.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);

    private PresenceService service(boolean privateMode) {
        when(redis.opsForSet()).thenReturn(sets);
        when(redis.opsForValue()).thenReturn(values);
        when(sets.isMember("privacy:private_mode", "u1")).thenReturn(privateMode);
        when(redis.hasKey("presence:u1")).thenReturn(true);
        when(values.get("lastseen:u1")).thenReturn("2026-11-18T10:00:00Z");
        return new PresenceService(redis);
    }

    @Test
    void privateModeLooksOfflineWithNoLastSeen() {
        PresenceResponse p = service(true).presenceOf("u1");
        assertThat(p.online()).isFalse();
        assertThat(p.lastSeen()).isNull();
    }

    @Test
    void otherwisePresenceIsShown() {
        PresenceResponse p = service(false).presenceOf("u1");
        assertThat(p.online()).isTrue();
        assertThat(p.lastSeen()).isNotNull();
    }
}
