package com.softlaunch.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "match-service")
public interface MatchClient {

    @GetMapping("/internal/matches/{matchId}")
    MatchSummary getMatch(@PathVariable("matchId") UUID matchId);
}