package com.softlaunch.ai.vibe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * INTERNAL only (/internal is not routed by the gateway). Returns scores, never the embeddings themselves.
 */
@RestController
@RequestMapping("/internal/ai")
public class VibeController {

    private final EmbeddingService embeddingService;

    public VibeController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public record SimilarityRequest(@NotNull UUID userId, @NotNull @Size(max = 500) List<UUID> candidateIds) {
    }

    public record SimilarityResponse(Map<UUID, Double> scores) {
    }

    /** Called by discovery while building a feed. Candidates without an embedding are simply missing. */
    @PostMapping("/similarity")
    public SimilarityResponse similarity(@Valid @RequestBody SimilarityRequest request) {
        return new SimilarityResponse(embeddingService.similarity(request.userId(), request.candidateIds()));
    }

    @PostMapping("/embeddings/backfill")
    public Map<String, Integer> backfill() {
        return embeddingService.backfill();
    }
}
