package com.softlaunch.safety.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "match-service")
public interface MatchClient {

    record MatchSummary(UUID matchId, UUID userAId, UUID userBId) {
        public boolean isBetween(UUID one, UUID two) {
            return (one.equals(userAId) && two.equals(userBId)) || (one.equals(userBId) && two.equals(userAId));
        }
    }

    @GetMapping("/internal/matches/{matchId}")
    MatchSummary getMatch(@PathVariable("matchId") UUID matchId);
}
