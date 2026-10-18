package com.softlaunch.chat.config;

import com.softlaunch.chat.model.Message;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MongoIndexes implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;

    public MongoIndexes(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        var indexOps = mongoTemplate.indexOps(Message.class);
        indexOps.createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));
        indexOps.createIndex(new Index().on("matchId", Sort.Direction.ASC).on("sentAt", Sort.Direction.DESC));
    }
}