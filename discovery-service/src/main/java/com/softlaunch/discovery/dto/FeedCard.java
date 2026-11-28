package com.softlaunch.discovery.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record FeedCard(
        UUID userId,
        String displayName,
        boolean verified,
        Integer age,
        int distanceKm,
        String bio,
        String city,
        Set<String> lookingFor,
        List<String> sharedInterests,
        List<String> photoUrls,
        List<String> reasons,
        int score,
        boolean veiled,
        List<String> mutualDesires,
        java.time.Instant crownedUntil) {   // King of the Hill: shown first, with a crown, until then (null = not crowned)

    /** Without a crown (how the feed is built and cached; crowns are applied on every read). */
    public FeedCard(UUID userId, String displayName, boolean verified, Integer age, int distanceKm, String bio, String city,
                    Set<String> lookingFor, List<String> sharedInterests, List<String> photoUrls, List<String> reasons,
                    int score, boolean veiled, List<String> mutualDesires) {
        this(userId, displayName, verified, age, distanceKm, bio, city, lookingFor, sharedInterests, photoUrls, reasons,
                score, veiled, mutualDesires, null);
    }

    public FeedCard crowned(java.time.Instant until) {
        return new FeedCard(userId, displayName, verified, age, distanceKm, bio, city, lookingFor, sharedInterests, photoUrls,
                reasons, score, veiled, mutualDesires, until);
    }   // desires you BOTH chose (into / open to); never anyone's "not into"   // photos are blurred copies until you match
}