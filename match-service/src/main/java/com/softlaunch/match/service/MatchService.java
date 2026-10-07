package com.softlaunch.match.service;

import com.softlaunch.match.dto.MatchResponse;
import com.softlaunch.match.event.MatchRemovedEvent;
import com.softlaunch.match.exception.MatchNotFoundException;
import com.softlaunch.match.model.Match;
import com.softlaunch.match.repository.MatchRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MatchService(MatchRepository matchRepository, ApplicationEventPublisher eventPublisher) {
        this.matchRepository = matchRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> myMatches(UUID me) {
        return matchRepository.findByUserAIdOrUserBIdOrderByCreatedAtDesc(me, me)
                .stream()
                .map(match -> MatchResponse.from(match, me))
                .toList();
    }

    @Transactional
    public void unmatch(UUID me, UUID matchId) {
        Match match = matchRepository.findById(matchId)
                .filter(m -> m.involves(me))
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        matchRepository.delete(match);
        eventPublisher.publishEvent(MatchRemovedEvent.of(
                match.getId(), match.getUserAId(), match.getUserBId(), me));
    }
}