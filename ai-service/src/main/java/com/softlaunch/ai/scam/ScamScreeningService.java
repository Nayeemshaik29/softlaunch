package com.softlaunch.ai.scam;

import com.softlaunch.ai.observability.ScamMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Rules first, LLM second, a moderator decides. Never blocks or bans anyone by itself.
 * Logs ids, scores and verdicts only, never the message text.
 */
@Service
public class ScamScreeningService {

    public static final String SCAM_SUSPECTED = "scam.suspected";
    static final double MIN_CONFIDENCE = 0.7;

    private static final Logger log = LoggerFactory.getLogger(ScamScreeningService.class);

    private final ScamClassifier classifier;
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper json;
    private final ScamMetrics metrics;

    public ScamScreeningService(ScamClassifier classifier, KafkaTemplate<String, String> kafka, JsonMapper json,
                                ScamMetrics metrics) {
        this.classifier = classifier;
        this.kafka = kafka;
        this.json = json;
        this.metrics = metrics;
    }

    public void screen(MessageScreeningEvent event) {
        ScamRules.Result rules = ScamRules.check(event.text());
        metrics.screened(rules);
        if (!rules.suspicious()) {
            return;   // the normal case: no LLM call at all
        }

        ScamClassifier.Verdict verdict;
        try {
            verdict = classifier.classify(event.text());
        } catch (Exception e) {
            if (rules.strong()) {
                log.warn("LLM unavailable ({}); rules are sure enough (score {}), reporting message {}",
                        e.getClass().getSimpleName(), rules.score(), event.messageId());
                metrics.verdict(ScamMetrics.SCAM_RULES_ONLY);
                report(event, rules, "OTHER", 0.0, "rules");
            } else {
                log.warn("LLM unavailable ({}); suspicious message {} (score {}) not reported",
                        e.getClass().getSimpleName(), event.messageId(), rules.score());
                metrics.verdict(ScamMetrics.SKIPPED_AI_UNAVAILABLE);
            }
            return;
        }

        if (verdict.scam() && verdict.confidence() >= MIN_CONFIDENCE) {
            log.info("Scam suspected in message {}: {} ({}), rule score {}", event.messageId(), verdict.category(),
                    verdict.confidence(), rules.score());
            metrics.verdict(ScamMetrics.SCAM_AI);
            report(event, rules, verdict.category(), verdict.confidence(), "ai");
        } else if (rules.signals().contains(ScamRules.Signal.MANIPULATION)) {
            // the message tried to talk the checker out of it, and the LLM went along: don't trust that answer
            log.warn("LLM said not a scam, but message {} tries to manipulate the checker (score {}): reporting anyway",
                    event.messageId(), rules.score());
            metrics.verdict(ScamMetrics.SCAM_RULES_OVERRIDE);
            report(event, rules, "OTHER", verdict.confidence(), "rules_override");
        } else {
            log.info("Message {} flagged by rules (score {}) but the LLM says not a scam ({}, {})",
                    event.messageId(), rules.score(), verdict.category(), verdict.confidence());
            metrics.verdict(ScamMetrics.CLEAN_AI);
        }
    }

    private void report(MessageScreeningEvent event, ScamRules.Result rules, String category, double confidence,
                        String source) {
        ScamSuspectedEvent suspected = new ScamSuspectedEvent(UUID.randomUUID(), event.messageId(), event.matchId(),
                event.senderId(), event.recipientId(), category, confidence, source,
                rules.signals().stream().map(Enum::name).collect(Collectors.toSet()), Instant.now());
        // key = senderId: all reports about one person go to one partition, in order
        kafka.send(SCAM_SUSPECTED, event.senderId().toString(), json.writeValueAsString(suspected));
    }
}
