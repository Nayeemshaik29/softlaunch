package com.softlaunch.user.config;

import com.softlaunch.user.exception.InvalidEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaErrorConfig {

    /**
     * Declared here too (not only in verification-service): whichever service starts first creates the topic
     * with 3 partitions. Otherwise the broker auto-creates it with 1, and a consumer that started early
     * only sees partition 0 until its metadata refresh (up to 5 minutes later).
     */
    @Bean
    public NewTopic userVerifiedTopic() {
        return TopicBuilder.name("user.verified").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userVerifiedDltTopic() {
        return TopicBuilder.name("user.verified.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
        handler.addNotRetryableExceptions(JacksonException.class, InvalidEventException.class);
        return handler;
    }
}
