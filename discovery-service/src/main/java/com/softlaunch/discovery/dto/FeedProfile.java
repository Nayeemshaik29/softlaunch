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
        String gender,
        Set<String> interestedIn,
        Integer ageMin,
        Integer ageMax,
        Set<String> lookingFor,
        Set<String> interests,
        List<String> photoUrls) {
}