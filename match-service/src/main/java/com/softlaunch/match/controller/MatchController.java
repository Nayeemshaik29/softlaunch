package com.softlaunch.match.controller;

import com.softlaunch.match.dto.MatchResponse;
import com.softlaunch.match.repository.MatchRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchRepository matchRepository;

    public MatchController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<MatchResponse> myMatches(@RequestHeader("X-User-Id") UUID me) {
        return matchRepository.findByUserAIdOrUserBIdOrderByCreatedAtDesc(me, me)
                .stream()
                .map(match -> MatchResponse.from(match, me))
                .toList();
    }
}