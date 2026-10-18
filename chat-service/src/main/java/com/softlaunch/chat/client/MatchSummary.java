package com.softlaunch.chat.client;

import java.util.UUID;

public record MatchSummary(UUID matchId, UUID userAId, UUID userBId) {

    public boolean involves(UUID userId) {
        return userId.equals(userAId) || userId.equals(userBId);
    }
}