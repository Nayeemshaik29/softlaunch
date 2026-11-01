package com.softlaunch.chat.event;

import com.softlaunch.chat.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public MessageEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void publish(MessageSentEvent event) {
        String payload = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopicConfig.MESSAGE_SENT, event.matchId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish message.sent for {}", event.messageId(), ex);
                    } else {
                        log.info("Published message.sent {} (recipient online: {}) to partition {}, offset {}",
                                event.messageId(), event.recipientOnline(),
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
