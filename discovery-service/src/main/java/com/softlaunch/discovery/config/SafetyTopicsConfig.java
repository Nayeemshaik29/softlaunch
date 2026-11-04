package com.softlaunch.discovery.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Consumers declare their topics too, so they exist with 3 partitions whichever service starts first. */
@Configuration
public class SafetyTopicsConfig {

    @Bean
    public NewTopic blockChangedTopic() {
        return TopicBuilder.name("block.changed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic reviewStatusTopic() {
        return TopicBuilder.name("user.review_status").partitions(3).replicas(1).build();
    }
}
