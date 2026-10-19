package com.softlaunch.chat.service;

import com.softlaunch.chat.client.MatchClient;
import com.softlaunch.chat.client.MatchSummary;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
public class ParticipantGuard {

    private final MatchClient matchClient;

    public ParticipantGuard(MatchClient matchClient) {
        this.matchClient = matchClient;
    }

    public void requireParticipant(UUID me, UUID matchId) {
        MatchSummary match;
        try {
            match = matchClient.getMatch(matchId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chat temporarily unavailable");
        }
        if (!match.involves(me)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        }
    }
}