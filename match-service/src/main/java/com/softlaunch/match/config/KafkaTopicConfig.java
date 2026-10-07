package com.softlaunch.match.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String MATCH_CREATED = "match.created";

    @Bean
    public NewTopic matchCreatedTopic() {
        return TopicBuilder.name(MATCH_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}