package com.softlaunch.chat.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String MESSAGE_SENT = "message.sent";
    public static final String SCREENSHOT_TAKEN = "screenshot.taken";
    public static final String MESSAGE_SCREENING = "message.screening";

    @Bean
    public NewTopic screenshotTakenTopic() {
        return TopicBuilder.name(SCREENSHOT_TAKEN)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /** Carries message TEXT: Kafka deletes it after 24h (same as the chat message), only ai-service reads it. */
    @Bean
    public NewTopic messageScreeningTopic() {
        return TopicBuilder.name(MESSAGE_SCREENING)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", String.valueOf(java.time.Duration.ofHours(24).toMillis()))
                .build();
    }

    @Bean
    public NewTopic messageSentTopic() {
        return TopicBuilder.name(MESSAGE_SENT)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
