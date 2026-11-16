package com.softlaunch.ai.observability;

import com.softlaunch.ai.scam.ScamRules;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * softlaunch_scam_screened_total               every message checked
 * softlaunch_scam_signals_total{signal}         which rules fire (tuning the rules)
 * softlaunch_scam_verdicts_total{verdict}       what happened to suspicious messages:
 *   clean_ai (LLM: not a scam), scam_ai (reported), scam_rules_only (LLM down, rules strong: reported),
 *   scam_rules_override (LLM said clean, but the message tried to manipulate the checker: reported),
 *   skipped_ai_unavailable (LLM down, rules not strong: not reported)
 * The share of messages that reach the LLM = verdicts / screened. Should stay small (a few %).
 */
@Component
public class ScamMetrics {

    public static final String CLEAN_AI = "clean_ai";
    public static final String SCAM_AI = "scam_ai";
    public static final String SCAM_RULES_ONLY = "scam_rules_only";
    public static final String SCAM_RULES_OVERRIDE = "scam_rules_override";
    public static final String SKIPPED_AI_UNAVAILABLE = "skipped_ai_unavailable";

    private final MeterRegistry registry;
    private final Counter screened;

    public ScamMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.screened = Counter.builder("softlaunch.scam.screened").description("Chat messages screened").register(registry);
        for (ScamRules.Signal s : ScamRules.Signal.values()) signal(s);
        List.of(CLEAN_AI, SCAM_AI, SCAM_RULES_ONLY, SCAM_RULES_OVERRIDE, SKIPPED_AI_UNAVAILABLE).forEach(this::verdictCounter);
    }

    private Counter signal(ScamRules.Signal s) {
        return Counter.builder("softlaunch.scam.signals").tag("signal", s.name()).register(registry);
    }

    private Counter verdictCounter(String verdict) {
        return Counter.builder("softlaunch.scam.verdicts").tag("verdict", verdict).register(registry);
    }

    public void screened(ScamRules.Result result) {
        screened.increment();
        result.signals().forEach(s -> signal(s).increment());
    }

    public void verdict(String verdict) {
        verdictCounter(verdict).increment();
    }
}
