package com.softlaunch.discovery.service;

import com.softlaunch.discovery.client.SwipedUsersClient;
import com.softlaunch.discovery.dto.FeedCard;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PrivateModeFilterTest {

    @SuppressWarnings("unchecked")
    private final SetOperations<String, String> sets = mock(SetOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final SwipedUsersClient swipes = mock(SwipedUsersClient.class);
    private final UUID me = UUID.randomUUID();
    private final FeedCard open = card(), hidden = card(), likedMe = card();
    private final java.util.Set<Object> veiledIds = new java.util.HashSet<>();

    private static FeedCard card() {
        return new FeedCard(UUID.randomUUID(), "x", false, 30, 2, null, null, Set.of(), List.of(), List.of("https://s3/real.jpg"), List.of(), 50, false, List.of());
    }

    @Test
    void someoneWhoJustVeiledLosesTheirRealLinksEvenInACachedFeed() {
        veiledIds.add(open.userId().toString());
        List<FeedCard> out = filter().apply(me, List.of(open));
        assertThat(out.get(0).photoUrls()).isEmpty();
        assertThat(out.get(0).veiled()).isTrue();
    }

    private PrivateModeFilter filter(FeedCard... privateOnes) {
        when(redis.opsForSet()).thenReturn(sets);
        when(sets.isMember(eq(PrivateModeFilter.VEILED_KEY), any(Object[].class))).thenAnswer(inv -> {
            Map<Object, Boolean> m = new HashMap<>();
            Object[] args = inv.getArguments();
            for (int i = 1; i < args.length; i++) m.put(args[i], veiledIds.contains(args[i]));
            return m;
        });
        when(sets.isMember(eq(PrivateModeFilter.KEY), any(Object[].class))).thenAnswer(inv -> {
            Map<Object, Boolean> m = new HashMap<>();
            Object[] args = inv.getArguments();   // Mockito spreads varargs: [key, id1, id2, ...]
            for (int i = 1; i < args.length; i++) m.put(args[i], false);
            for (FeedCard c : privateOnes) m.put(c.userId().toString(), true);
            return m;
        });
        return new PrivateModeFilter(redis, swipes);
    }

    @Test
    void privatePeopleAreShownOnlyToPeopleTheyLiked() {
        when(swipes.likedBy(me)).thenReturn(Set.of(likedMe.userId().toString()));
        List<FeedCard> out = filter(hidden, likedMe).apply(me, List.of(open, hidden, likedMe));
        assertThat(out).containsExactly(open, likedMe);
    }

    @Test
    void noPrivateCardsMeansNoExtraCallToMatchService() {
        assertThat(filter().apply(me, List.of(open))).containsExactly(open);   // not veiled either: unchanged
        verifyNoInteractions(swipes);
    }

    @Test
    void matchServiceDownHidesPrivatePeople() {
        when(swipes.likedBy(me)).thenReturn(Set.of());   // fails closed
        assertThat(filter(hidden).apply(me, List.of(open, hidden))).containsExactly(open);
    }
}
