package com.softlaunch.chat.call;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** The money rules for calls, against a tiny in-memory Redis. */
class CallPaymentRulesTest {

    private final Map<String, String> kv = new HashMap<>();
    private final Map<String, Set<String>> sets = new HashMap<>();
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private Instant now = Instant.parse("2026-11-28T06:00:00Z");   // 11:30 IST
    private CallRules rules;
    private final UUID arjun = UUID.randomUUID(), priya = UUID.randomUUID(), match = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ValueOperations<String, String> v = mock(ValueOperations.class);
        SetOperations<String, String> so = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(v);
        when(redis.opsForSet()).thenReturn(so);
        when(redis.hasKey(anyString())).thenAnswer(i -> kv.containsKey(i.<String>getArgument(0)));
        when(v.get(anyString())).thenAnswer(i -> kv.get(i.<String>getArgument(0)));
        when(v.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(i -> kv.putIfAbsent(i.getArgument(0), i.getArgument(1)) == null);
        when(so.add(anyString(), any(String[].class))).thenAnswer(i -> { sets.computeIfAbsent(i.getArgument(0), k -> new HashSet<>()).add(i.getArgument(1)); return 1L; });
        when(so.size(anyString())).thenAnswer(i -> (long) sets.getOrDefault(i.<String>getArgument(0), Set.of()).size());
        rules = new CallRules(redis, new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId z) { return Clock.fixed(now, z); }
            public Instant instant() { return now; }
        });
    }

    @Test
    void firstDayIsFreeThenEachManNeedsAPass() {
        assertThat(rules.problem(arjun, priya, match, false)).isNull();   // never called: free
        rules.callAnswered(match, false);
        now = now.plus(Duration.ofHours(23));
        assertThat(rules.problem(arjun, priya, match, false)).isNull();   // still inside 24h
        now = now.plus(Duration.ofHours(2));
        assertThat(rules.problem(arjun, priya, match, false)).isEqualTo("pass_needed");
        kv.put("freetier:" + priya, "1");                                  // verified woman: free
        kv.put("callpass:" + match + ":" + arjun, "1");                    // Arjun bought today's pass
        assertThat(rules.problem(arjun, priya, match, false)).isNull();
        kv.remove("freetier:" + priya);
        assertThat(rules.problem(arjun, priya, match, false)).isEqualTo("their_pass_needed");
    }

    @Test
    void videoUnlocksAfterThreeCallDaysAndTheNextVideoDayIsFree() {
        kv.put("callpass:" + match + ":" + arjun, "1");
        kv.put("callpass:" + match + ":" + priya, "1");
        for (int d = 0; d < 2; d++) {
            rules.callAnswered(match, false);
            assertThat(rules.problem(arjun, priya, match, true)).isEqualTo("video_locked");
            now = now.plus(Duration.ofDays(1));
        }
        rules.callAnswered(match, false);                                  // 3rd different day
        now = now.plus(Duration.ofDays(1));                                // day 4
        kv.clear();                                                         // no passes at all today…
        kv.put("call:first:" + match, Long.toString(now.minus(Duration.ofDays(3)).toEpochMilli()));
        assertThat(rules.problem(arjun, priya, match, true)).isNull();     // …video is still free today
        rules.callAnswered(match, true);
        now = now.plus(Duration.ofDays(1));                                // day 5: back to passes
        assertThat(rules.problem(arjun, priya, match, true)).isEqualTo("pass_needed");
    }
}
