package com.softlaunch.verification.service;

import com.softlaunch.verification.model.OutboxEvent;
import com.softlaunch.verification.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Background job: moves outbox rows to Kafka. At-least-once, so consumers must be idempotent. */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${softlaunch.outbox.poll-interval:2s}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = outboxRepository.lockNextBatch(BATCH_SIZE);
        for (OutboxEvent event : batch) {
            try {
                // wait for Kafka's "got it" before marking the row as sent
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(10, TimeUnit.SECONDS);
                event.markPublished();
                log.info("Outbox published {} {} (attempt {})", event.getTopic(), event.getId(), event.getAttempts());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException | TimeoutException | RuntimeException e) {
                event.recordFailure();
                log.warn("Outbox publish failed for {} (attempt {}), will retry: {}",
                        event.getId(), event.getAttempts(), e.getMessage());
                break;   // Kafka is probably down: stop this batch, keep order, try again next tick
            }
        }
    }
}
