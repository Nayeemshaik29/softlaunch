package com.softlaunch.discovery.client;

import com.softlaunch.discovery.dto.FeedProfile;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserProfileClient {

    @PostMapping("/internal/users/profiles")
    List<FeedProfile> getProfiles(@RequestBody List<UUID> userIds);
}