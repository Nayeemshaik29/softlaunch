package com.softlaunch.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String PROFILE_UPDATED = "profile.updated";

    @Bean
    public NewTopic profileUpdatedTopic() {
        return TopicBuilder.name(PROFILE_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}