package com.softlaunch.ai.vibe;

import com.softlaunch.ai.client.UserProfileClient;
import com.softlaunch.ai.client.UserProfileClient.ProfileView;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps one embedding per profile in ai-db, and answers "how similar are these people to me?".
 * Logs ids and outcomes only, never profile text.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int BACKFILL_PAGE = 100;

    private final UserProfileClient users;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingRepository repository;
    private final MeterRegistry registry;
    private final String modelName;

    public EmbeddingService(UserProfileClient users, EmbeddingModel embeddingModel, EmbeddingRepository repository,
                            MeterRegistry registry, @Value("${spring.ai.ollama.embedding.model}") String modelName) {
        this.users = users;
        this.embeddingModel = embeddingModel;
        this.repository = repository;
        this.registry = registry;
        this.modelName = modelName;
        for (String r : List.of("embedded", "unchanged", "empty", "no_profile")) count(r, 0);
    }

    /** Called on profile.updated and by the backfill. Skips the model call when the text hasn't changed. */
    public String refresh(UUID userId) {
        List<ProfileView> found = users.getProfiles(List.of(userId));
        if (found.isEmpty()) {
            repository.delete(userId);
            return count("no_profile", 1);
        }
        String text = ProfileText.of(found.getFirst());
        if (text.isBlank()) {
            repository.delete(userId);   // nothing meaningful to compare yet
            return count("empty", 1);
        }
        String hash = ProfileText.sha256(ProfileText.TASK_PREFIX + modelName + "\n" + text);
        if (repository.contentHash(userId).filter(hash::equals).isPresent()) {
            return count("unchanged", 1);   // e.g. only a photo changed: no model call
        }
        float[] vector = embeddingModel.embed(ProfileText.TASK_PREFIX + text);
        repository.upsert(userId, vector, hash, modelName);
        log.info("Embedded profile of {} ({} dimensions)", userId, vector.length);
        return count("embedded", 1);
    }

    public Map<UUID, Double> similarity(UUID me, Collection<UUID> candidates) {
        return repository.similarity(me, candidates);
    }

    /** One-time job for users who signed up before this feature existed. Safe to run again (unchanged = skipped). */
    public Map<String, Integer> backfill() {
        Map<String, Integer> totals = new java.util.TreeMap<>();
        for (int page = 0; ; page++) {
            List<UUID> ids = users.profileIds(page, BACKFILL_PAGE);
            for (UUID id : ids) {
                try {
                    totals.merge(refresh(id), 1, Integer::sum);
                } catch (RuntimeException e) {
                    log.warn("Backfill failed for {}: {}", id, e.getClass().getSimpleName());
                    totals.merge("failed", 1, Integer::sum);
                }
            }
            if (ids.size() < BACKFILL_PAGE) break;
        }
        log.info("Embedding backfill finished: {} (rows now {})", totals, repository.count());
        return totals;
    }

    private String count(String result, int n) {
        registry.counter("softlaunch.ai.embeddings", "result", result).increment(n);
        return result;
    }
}
