package com.softlaunch.discovery.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record FeedProfile(
        UUID userId,
        String displayName,
        Integer age,
        String bio,
        String city,
        Set<String> lookingFor,
        Set<String> interests,
        List<String> photoUrls) {
}