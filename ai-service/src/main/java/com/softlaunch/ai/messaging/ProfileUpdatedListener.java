package com.softlaunch.ai.messaging;

import com.softlaunch.ai.vibe.EmbeddingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/** Re-embeds a profile whenever it changes. If Ollama is down: retried twice, then the DLT; the backfill repairs it. */
@Component
public class ProfileUpdatedListener {

    private final EmbeddingService embeddingService;
    private final JsonMapper json;

    public ProfileUpdatedListener(EmbeddingService embeddingService, JsonMapper json) {
        this.embeddingService = embeddingService;
        this.json = json;
    }

    @KafkaListener(topics = "profile.updated")
    public void onProfileUpdated(String payload) {
        JsonNode node = json.readTree(payload);
        JsonNode userId = node.get("userId");
        if (userId == null || userId.isNull()) {
            throw new IllegalArgumentException("profile.updated without userId");
        }
        embeddingService.refresh(UUID.fromString(userId.asString()));
    }
}
