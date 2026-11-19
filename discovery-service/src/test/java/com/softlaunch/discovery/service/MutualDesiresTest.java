package com.softlaunch.discovery.service;

import com.softlaunch.discovery.dto.FeedProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MutualDesiresTest {

    private static FeedProfile p(Set<String> into, Set<String> open, Set<String> not) {
        return new FeedProfile(UUID.randomUUID(), "x", false, 30, null, null, "WOMAN", Set.of(), null, null, Set.of(), Set.of(),
                List.of(), false, false, into, open, not);
    }

    @Test
    void onlySharedDesiresAppear() {
        var me = p(Set.of("ROMANCE", "MASSAGES"), Set.of("DRESSING_UP"), Set.of());
        var them = p(Set.of("MASSAGES"), Set.of("ROMANCE", "SLOW_DANCING"), Set.of());
        assertThat(FeedService.mutualDesires(me, them)).containsExactly("MASSAGES", "ROMANCE");
    }

    @Test
    void anythingEitherPersonIsNotIntoNeverShows() {
        var me = p(Set.of("PLAYFUL_TEASING"), Set.of("DRESSING_UP"), Set.of());
        var them = p(Set.of("DRESSING_UP"), Set.of("PLAYFUL_TEASING"), Set.of("PLAYFUL_TEASING"));
        assertThat(FeedService.mutualDesires(me, them)).containsExactly("DRESSING_UP");
    }

    @Test
    void peopleWithoutDesiresShareNothing() {
        assertThat(FeedService.mutualDesires(p(null, null, null), p(Set.of("ROMANCE"), Set.of(), Set.of()))).isEmpty();
    }
}
