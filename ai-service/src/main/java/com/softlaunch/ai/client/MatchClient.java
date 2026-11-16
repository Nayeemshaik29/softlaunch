package com.softlaunch.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "match-service")
public interface MatchClient {

    @GetMapping("/internal/matches/{matchId}")
    MatchSummary getMatch(@PathVariable UUID matchId);

    record MatchSummary(UUID matchId, UUID userAId, UUID userBId) {

        public boolean includes(UUID userId) {
            return userAId.equals(userId) || userBId.equals(userId);
        }

        public UUID otherThan(UUID me) {
            return userAId.equals(me) ? userBId : userAId;
        }
    }
}
