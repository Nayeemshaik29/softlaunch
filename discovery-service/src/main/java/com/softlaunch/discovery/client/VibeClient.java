package com.softlaunch.discovery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** ai-service: "how similar is each candidate's profile to mine?" (0..1). Timeouts are tiny on purpose, see config. */
@FeignClient(name = "ai-service")
public interface VibeClient {

    @PostMapping("/internal/ai/similarity")
    SimilarityResponse similarity(@RequestBody SimilarityRequest request);

    record SimilarityRequest(UUID userId, List<UUID> candidateIds) {
    }

    record SimilarityResponse(Map<UUID, Double> scores) {
    }
}
