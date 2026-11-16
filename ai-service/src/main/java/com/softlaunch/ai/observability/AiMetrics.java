package com.softlaunch.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * softlaunch_ai_openers_total{source, reason}: where openers came from and why we fell back.
 *   source=ai | cache | fallback;  reason=ok | timeout | error | unsafe_output
 * softlaunch_ai_openers_rejected_total: single AI suggestions dropped by OpenerSafety (rising = model misbehaving).
 * LLM latency and token counts come for free from Spring AI (gen_ai_client_operation_seconds, gen_ai_client_token_usage).
 */
@Component
public class AiMetrics {

    public static final String OK = "ok";
    public static final String TIMEOUT = "timeout";
    public static final String ERROR = "error";
    public static final String UNSAFE_OUTPUT = "unsafe_output";

    private final MeterRegistry registry;
    private final Counter rejected;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.rejected = Counter.builder("softlaunch.ai.openers.rejected")
                .description("AI openers dropped by the safety filter").register(registry);
        served("ai", OK);
        served("cache", OK);
        List.of(TIMEOUT, ERROR, UNSAFE_OUTPUT).forEach(reason -> served("fallback", reason));
    }

    private Counter served(String source, String reason) {
        return Counter.builder("softlaunch.ai.openers")
                .description("Opener requests answered, by source")
                .tag("source", source).tag("reason", reason)
                .register(registry);
    }

    public void servedFromAi() { served("ai", OK).increment(); }

    public void servedFromCache() { served("cache", OK).increment(); }

    public void servedFallback(String reason) { served("fallback", reason).increment(); }

    public void rejected(int count) { rejected.increment(count); }
}
