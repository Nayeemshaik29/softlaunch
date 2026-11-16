package com.softlaunch.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
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
    record ProfileView(UUID userId, String displayName, String bio, String city, Set<String> interests) {
    }
}
