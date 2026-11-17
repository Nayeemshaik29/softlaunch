package com.softlaunch.ai.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Checks every 15s whether Ollama answers AND has the models we need (a missing model fails just as
 * badly as Ollama being down). Result is cached: health checks and Prometheus read it without calling Ollama.
 * Gauge softlaunch_ai_ollama_up = 1 / 0, used by the "Ollama unreachable" alert.
 */
@Component
public class OllamaStatus {

    public record Snapshot(boolean up, String problem, Set<String> missingModels, Instant checkedAt) {
    }

    private final RestClient client;
    private final List<String> requiredModels;
    private final AtomicReference<Snapshot> last =
            new AtomicReference<>(new Snapshot(false, "not checked yet", Set.of(), Instant.EPOCH));

    public OllamaStatus(@Value("${spring.ai.ollama.base-url}") String baseUrl,
                        @Value("${spring.ai.ollama.chat.model}") String chatModel,
                        @Value("${spring.ai.ollama.embedding.model}") String embeddingModel,
                        MeterRegistry registry) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.requiredModels = List.of(chatModel, embeddingModel);
        Gauge.builder("softlaunch.ai.ollama.up", last, s -> s.get().up() ? 1 : 0)
                .description("1 = Ollama reachable with all required models").register(registry);
    }

    @Scheduled(initialDelay = 0, fixedDelay = 15_000)   // check at startup right away
    public void check() {
        try {
            Map<?, ?> tags = client.get().uri("/api/tags").retrieve().body(Map.class);
            Set<String> installed = ((List<?>) tags.get("models")).stream()
                    .map(m -> String.valueOf(((Map<?, ?>) m).get("name")))
                    .collect(Collectors.toSet());
            Set<String> missing = requiredModels.stream()
                    .filter(model -> !installed.contains(model) && !installed.contains(model + ":latest"))
                    .collect(Collectors.toSet());
            last.set(missing.isEmpty()
                    ? new Snapshot(true, null, Set.of(), Instant.now())
                    : new Snapshot(false, "required models not installed", missing, Instant.now()));
        } catch (RuntimeException e) {
            last.set(new Snapshot(false, "unreachable (" + e.getClass().getSimpleName() + ")", Set.of(), Instant.now()));
        }
    }

    public Snapshot snapshot() {
        return last.get();
    }
}
