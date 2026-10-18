package com.softlaunch.match.controller;

import com.softlaunch.match.dto.MatchSummary;
import com.softlaunch.match.exception.MatchNotFoundException;
import com.softlaunch.match.repository.MatchRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/matches")
public class InternalMatchController {

    private final MatchRepository matchRepository;

    public InternalMatchController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @GetMapping("/{matchId}")
    @Transactional(readOnly = true)
    public MatchSummary getMatch(@PathVariable UUID matchId) {
        return matchRepository.findById(matchId)
                .map(m -> new MatchSummary(m.getId(), m.getUserAId(), m.getUserBId()))
                .orElseThrow(() -> new MatchNotFoundException(matchId));
    }
}