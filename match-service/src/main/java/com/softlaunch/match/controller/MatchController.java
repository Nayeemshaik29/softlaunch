package com.softlaunch.match.controller;

import com.softlaunch.match.dto.MatchResponse;
import com.softlaunch.match.service.MatchService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchResponse> myMatches(@RequestHeader("X-User-Id") UUID me) {
        return matchService.myMatches(me);
    }

    @DeleteMapping("/{matchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unmatch(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID matchId) {
        matchService.unmatch(me, matchId);
    }
}