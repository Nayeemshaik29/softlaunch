package com.softlaunch.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;
import java.util.UUID;

/** user-service's ❤ wallet (internal endpoint, not reachable through the gateway). 402 = not enough hearts. */
@FeignClient(name = "user-service", contextId = "userHearts")
public interface UserHeartsClient {

    @PostMapping("/internal/users/{userId}/hearts/spend")
    void spend(@PathVariable("userId") UUID userId, @RequestBody Map<String, String> body);
}
