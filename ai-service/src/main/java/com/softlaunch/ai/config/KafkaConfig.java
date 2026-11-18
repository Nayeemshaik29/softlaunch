package com.softlaunch.ai.config;

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

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
class KafkaConfig {

    private static final String DAY = String.valueOf(Duration.ofHours(24).toMillis());

    /** Same definition as in chat-service, whichever starts first creates it: 3 partitions, text deleted after 24h. */
    @Bean
    NewTopic messageScreeningTopic() {
        return TopicBuilder.name("message.screening").partitions(3).replicas(1).config("retention.ms", DAY).build();
    }

    /** Failed screening events also contain text, so their DLT is deleted after 24h too. */
    @Bean
    NewTopic messageScreeningDltTopic() {
        return TopicBuilder.name("message.screening.DLT").partitions(3).replicas(1).config("retention.ms", DAY).build();
    }

    @Bean
    NewTopic scamSuspectedTopic() {
        return TopicBuilder.name("scam.suspected").partitions(3).replicas(1).build();
    }

    /** Published by user-service when an account is deleted (endpoint comes with the Play Store work). */
    @Bean
    NewTopic userDeletedTopic() {
        return TopicBuilder.name("user.deleted").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic userDeletedDltTopic() {   // found by the topic audit: the only DLT that was never declared
        return TopicBuilder.name("user.deleted.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic profileUpdatedDltTopic() {
        return TopicBuilder.name("profile.updated.DLT").partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
        handler.addNotRetryableExceptions(JacksonException.class, IllegalArgumentException.class);
        return handler;
    }
}
