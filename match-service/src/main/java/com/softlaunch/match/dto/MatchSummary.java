package com.softlaunch.match.dto;

import java.util.UUID;

public record MatchSummary(UUID matchId, UUID userAId, UUID userBId) {
}