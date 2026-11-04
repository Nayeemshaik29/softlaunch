package com.softlaunch.chat.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String MESSAGE_SENT = "message.sent";
    public static final String SCREENSHOT_TAKEN = "screenshot.taken";

    @Bean
    public NewTopic screenshotTakenTopic() {
        return TopicBuilder.name(SCREENSHOT_TAKEN)
                .partitions(3)
                .replicas(1)
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
