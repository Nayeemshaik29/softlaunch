package com.softlaunch.discovery.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Values from real nomic-embed-text measurements (see FeedService constants). */
class VibePointsTest {

    @Test
    void noScoreMeansNoBonus() {
        assertThat(FeedService.vibePoints(null)).isZero();   // ai-service down or no embedding yet
    }

    @Test
    void unrelatedProfilesGetNothing() {
        assertThat(FeedService.vibePoints(0.735)).isZero();   // trekker vs party person
    }

    @Test
    void similarProfilesGetMostOfTheBonus() {
        assertThat(FeedService.vibePoints(0.904)).isEqualTo(13);   // two trekkers, no shared words
    }

    @Test
    void bonusIsCappedAtFifteen() {
        assertThat(FeedService.vibePoints(0.99)).isEqualTo(15);
    }
}
