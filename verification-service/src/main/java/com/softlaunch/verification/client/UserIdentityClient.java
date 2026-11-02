package com.softlaunch.verification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserIdentityClient {

    @GetMapping("/internal/users/{userId}/identity")
    UserIdentity identity(@PathVariable("userId") UUID userId);
}
