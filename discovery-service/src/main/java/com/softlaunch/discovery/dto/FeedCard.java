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
        List<String> mutualDesires) {   // desires you BOTH chose (into / open to); never anyone's "not into"   // photos are blurred copies until you match
}