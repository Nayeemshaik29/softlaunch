package com.softlaunch.ai.messaging;

import com.softlaunch.ai.openers.OpenerService;
import com.softlaunch.ai.vibe.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/**
 * Forgetting is a feature: whatever the AI keeps about a person is removed when they leave.
 *  - user.deleted  -> their embedding + openers cached for them
 *  - match.removed -> openers cached for that match (they quote the other person's profile)
 * Both are idempotent: deleting something already gone is fine, so redelivery is harmless.
 */
@Component
public class DeletionListener {

    private static final Logger log = LoggerFactory.getLogger(DeletionListener.class);

    private final EmbeddingService embeddings;
    private final OpenerService openers;
    private final JsonMapper json;

    public DeletionListener(EmbeddingService embeddings, OpenerService openers, JsonMapper json) {
        this.embeddings = embeddings;
        this.openers = openers;
        this.json = json;
    }

    @KafkaListener(topics = "user.deleted")
    public void onUserDeleted(String payload) {
        UUID userId = uuid(payload, "userId");
        embeddings.forget(userId);
        long cached = openers.evictUser(userId);
        log.info("AI data of deleted user {} removed (embedding + {} cached opener sets)", userId, cached);
    }

    @KafkaListener(topics = "match.removed")
    public void onMatchRemoved(String payload) {
        UUID matchId = uuid(payload, "matchId");
        long cached = openers.evictMatch(matchId);
        if (cached > 0) {
            log.info("Removed {} cached opener sets of match {}", cached, matchId);
        }
    }

    private UUID uuid(String payload, String field) {
        JsonNode value = json.readTree(payload).get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("event without " + field);
        }
        return UUID.fromString(value.asString());
    }
}
