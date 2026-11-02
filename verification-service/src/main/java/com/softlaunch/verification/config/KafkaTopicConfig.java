package com.softlaunch.verification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String USER_VERIFIED = "user.verified";

    @Bean
    public NewTopic userVerifiedTopic() {
        return TopicBuilder.name(USER_VERIFIED).partitions(3).replicas(1).build();
    }
}
