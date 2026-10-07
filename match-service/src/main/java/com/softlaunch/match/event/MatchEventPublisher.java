package com.softlaunch.match.event;

import com.softlaunch.match.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MatchEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public MatchEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchCreated(MatchCreatedEvent event) {
        String payload = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(KafkaTopicConfig.MATCH_CREATED, event.matchId().toString(), payload)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish match.created for match {}", event.matchId(), error);
                    } else {
                        log.info("Published match.created for match {} → partition {}, offset {}",
                                event.matchId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}