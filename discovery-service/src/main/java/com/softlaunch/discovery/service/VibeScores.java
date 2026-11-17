package com.softlaunch.discovery.service;

import com.softlaunch.discovery.client.VibeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Optional extra signal for the feed. The feed must NEVER depend on the AI: any error or a reply slower
 * than ~300ms means "no vibe scores" and the feed is ranked exactly like before.
 */
@Component
public class VibeScores {

    private static final Logger log = LoggerFactory.getLogger(VibeScores.class);

    private final VibeClient vibeClient;

    public VibeScores(VibeClient vibeClient) {
        this.vibeClient = vibeClient;
    }

    public Map<UUID, Double> forCandidates(UUID me, List<UUID> candidates) {
        if (candidates.isEmpty()) return Map.of();
        try {
            Map<UUID, Double> scores = vibeClient.similarity(new VibeClient.SimilarityRequest(me, candidates)).scores();
            return scores == null ? Map.of() : scores;
        } catch (RuntimeException e) {
            log.warn("ai-service unavailable for vibe scores ({}), ranking without them", e.getClass().getSimpleName());
            return Map.of();
        }
    }
}
