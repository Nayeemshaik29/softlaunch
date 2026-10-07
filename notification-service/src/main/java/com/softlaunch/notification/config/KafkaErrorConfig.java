package com.softlaunch.notification.config;

import com.softlaunch.notification.exception.InvalidEventException;
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

    public static final String MATCH_CREATED_DLT = "match.created.DLT";

    @Bean
    public NewTopic matchCreatedDltTopic() {
        return TopicBuilder.name(MATCH_CREATED_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
        handler.addNotRetryableExceptions(JacksonException.class, InvalidEventException.class);
        return handler;
    }
    @Bean
    public NewTopic matchRemovedDltTopic() {
        return TopicBuilder.name("match.removed.DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }
}