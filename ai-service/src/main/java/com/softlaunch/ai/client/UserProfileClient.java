package com.softlaunch.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserProfileClient {

    @PostMapping("/internal/users/profiles")
    List<ProfileView> getProfiles(@RequestBody List<UUID> userIds);

    /**
     * Only the fields an opener needs. user-service sends more (age, gender, intents, photos...);
     * Jackson ignores the rest, so they never even reach this service's memory as named fields.
     */
    record ProfileView(UUID userId, String displayName, String bio, String city, Set<String> interests,
                       List<String> hangoutPlaces, List<PromptAnswer> prompts) {

        /** Short constructor for code/tests that only need the opener fields. */
        public ProfileView(UUID userId, String displayName, String bio, String city, Set<String> interests) {
            this(userId, displayName, bio, city, interests, List.of(), List.of());
        }
    }

    record PromptAnswer(String question, String answer) {
    }

    /** Pages through every user that has a profile (for the one-time embedding backfill). */
    @GetMapping("/internal/users/profile-ids")
    List<UUID> profileIds(@RequestParam("page") int page, @RequestParam("size") int size);
}
