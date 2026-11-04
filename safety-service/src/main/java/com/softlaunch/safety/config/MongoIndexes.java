package com.softlaunch.safety.config;

import com.softlaunch.safety.model.Block;
import com.softlaunch.safety.model.CaptureRecord;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.UserSafetyStatus;
import com.softlaunch.safety.model.ReportStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
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
        var reports = mongoTemplate.indexOps(Report.class);
        // Only ONE open report per reporter -> target. The DB enforces it, even if two requests race.
        reports.createIndex(new Index()
                .on("reporterId", Sort.Direction.ASC)
                .on("reportedUserId", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("status").is(ReportStatus.OPEN.name())))
                .named("one_open_report_per_pair"));
        // Counting distinct reporters of a user in the last 30 days
        reports.createIndex(new Index().on("reportedUserId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));
        // Moderator queue: open reports, most urgent first, oldest first
        reports.createIndex(new Index().on("status", Sort.Direction.ASC).on("priorityRank", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC));
        // Retention: delete the whole report when expiresAt passes
        reports.createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));
        // "My reports"
        reports.createIndex(new Index().on("reporterId", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC));

        var blocks = mongoTemplate.indexOps(Block.class);
        // one document per direction; re-blocking reuses it
        blocks.createIndex(new Index().on("blockerId", Sort.Direction.ASC).on("blockedId", Sort.Direction.ASC)
                .unique().named("one_block_per_direction"));

        var captures = mongoTemplate.indexOps(CaptureRecord.class);
        captures.createIndex(new Index().on("takerId", Sort.Direction.ASC).on("occurredAt", Sort.Direction.DESC));
        captures.createIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));

        // outbox scans: only documents that still have something to send
        for (Class<?> type : new Class<?>[]{Block.class, UserSafetyStatus.class}) {
            mongoTemplate.indexOps(type).createIndex(new Index().on("pendingEvent.createdAt", Sort.Direction.ASC)
                    .sparse().named("pending_events"));
        }
    }
}
