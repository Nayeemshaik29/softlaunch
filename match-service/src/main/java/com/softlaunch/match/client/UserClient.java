package com.softlaunch.match.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserSummary getUser(@PathVariable("userId") UUID userId);

    /** Spend ❤ (user-service wallet). 402 when the balance is too low. */
    @org.springframework.web.bind.annotation.PostMapping("/internal/users/{userId}/hearts/spend")
    void spendHearts(@PathVariable("userId") UUID userId, @org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> body);
}