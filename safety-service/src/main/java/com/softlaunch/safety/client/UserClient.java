package com.softlaunch.safety.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    record UserSummary(UUID id, String displayName) { }

    @GetMapping("/internal/users/{userId}")
    UserSummary getUser(@PathVariable("userId") UUID userId);
}
