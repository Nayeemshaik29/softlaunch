package com.softlaunch.safety.outbox;

import com.softlaunch.safety.model.Block;
import com.softlaunch.safety.model.UserSafetyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Every 2s: find documents with a pendingEvent, send it to Kafka, then remove it from the document. */
@Component
public class MongoOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(MongoOutboxPublisher.class);
    private static final List<Class<? extends HasPendingEvent>> SOURCES = List.of(Block.class, UserSafetyStatus.class);

    private final MongoTemplate mongoTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public MongoOutboxPublisher(MongoTemplate mongoTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${softlaunch.outbox.poll-interval:2s}")
    public void publishPending() {
        for (Class<? extends HasPendingEvent> type : SOURCES) {
            Query waiting = Query.query(Criteria.where("pendingEvent").ne(null))
                    .with(org.springframework.data.domain.Sort.by("pendingEvent.createdAt")).limit(50);
            for (HasPendingEvent doc : mongoTemplate.find(waiting, type)) {
                PendingEvent event = doc.getPendingEvent();
                try {
                    kafkaTemplate.send(event.topic(), event.key(), event.payload()).get(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (ExecutionException | TimeoutException | RuntimeException e) {
                    log.warn("Outbox publish failed for {} {}, will retry: {}", event.topic(), event.eventId(), e.getMessage());
                    return;   // Kafka is probably down: try again next tick
                }
                // Clear ONLY this event. If the document got a newer event meanwhile, keep that one.
                mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(doc.outboxId()).and("pendingEvent.eventId").is(event.eventId())),
                        new Update().unset("pendingEvent"), type);
                log.info("Outbox published {} {}", event.topic(), event.eventId());
            }
        }
    }
}
