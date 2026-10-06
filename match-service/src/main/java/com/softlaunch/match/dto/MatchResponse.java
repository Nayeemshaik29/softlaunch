package com.softlaunch.match.dto;

import com.softlaunch.match.model.Match;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(UUID matchId, UUID otherUserId, Instant matchedAt) {

    public static MatchResponse from(Match match, UUID me) {
        return new MatchResponse(match.getId(), match.otherUser(me), match.getCreatedAt());
    }
}