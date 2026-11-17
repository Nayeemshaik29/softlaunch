package com.softlaunch.ai.vibe;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Plain SQL on purpose: pgvector's "vector" type and "<=>" operator are clearer in SQL than hidden behind JPA. */
@Repository
class EmbeddingRepository {

    private final JdbcClient jdbc;

    EmbeddingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Optional<String> contentHash(UUID userId) {
        return jdbc.sql("SELECT content_hash FROM profile_embeddings WHERE user_id = :id")
                .param("id", userId).query(String.class).optional();
    }

    void upsert(UUID userId, float[] embedding, String contentHash, String model) {
        jdbc.sql("""
                INSERT INTO profile_embeddings (user_id, embedding, content_hash, model, updated_at)
                VALUES (:id, CAST(:embedding AS vector), :hash, :model, :now)
                ON CONFLICT (user_id) DO UPDATE
                   SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash,
                       model = EXCLUDED.model, updated_at = EXCLUDED.updated_at
                """)
                .param("id", userId).param("embedding", toLiteral(embedding)).param("hash", contentHash)
                .param("model", model).param("now", Timestamp.from(Instant.now()))
                .update();
    }

    void delete(UUID userId) {
        jdbc.sql("DELETE FROM profile_embeddings WHERE user_id = :id").param("id", userId).update();
    }

    /**
     * Cosine similarity (1 = same meaning) between me and each candidate that has an embedding.
     * "<=>" is pgvector's cosine DISTANCE, so similarity = 1 - distance. Computed inside Postgres:
     * the vectors never leave the database.
     */
    Map<UUID, Double> similarity(UUID me, Collection<UUID> candidates) {
        Map<UUID, Double> scores = new HashMap<>();
        if (candidates.isEmpty()) return scores;
        jdbc.sql("""
                SELECT c.user_id, 1 - (c.embedding <=> m.embedding) AS similarity
                  FROM profile_embeddings m
                  JOIN profile_embeddings c ON c.user_id IN (:candidates)
                 WHERE m.user_id = :me
                """)
                .param("me", me).param("candidates", candidates)
                .query((rs, n) -> scores.put(rs.getObject("user_id", UUID.class), rs.getDouble("similarity")))
                .list();
        return scores;
    }

    long count() {
        return jdbc.sql("SELECT count(*) FROM profile_embeddings").query(Long.class).single();
    }

    /** pgvector's text format: [0.1,0.2,...] */
    private static String toLiteral(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 10).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
