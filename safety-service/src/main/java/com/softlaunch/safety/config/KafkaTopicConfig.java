package com.softlaunch.safety.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /** One topic for block AND unblock, keyed by the user pair, so Kafka keeps them in order. */
    public static final String BLOCK_CHANGED = "block.changed";
    /** Someone went under review or was cleared. Keyed by userId. */
    public static final String REVIEW_STATUS = "user.review_status";

    @Bean
    public NewTopic blockChangedTopic() {
        return TopicBuilder.name(BLOCK_CHANGED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic reviewStatusTopic() {
        return TopicBuilder.name(REVIEW_STATUS).partitions(3).replicas(1).build();
    }
}
