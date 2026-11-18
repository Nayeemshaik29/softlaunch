package com.softlaunch.safety.service;

import com.softlaunch.safety.model.Block;
import com.softlaunch.safety.model.CaptureRecord;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.UserSafetyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Account deletion vs. safety: we must forget the person, but not make abuse disappear.
 *  - reports ABOUT them: kept (evidence for moderators), deleted by the normal 365-day retention
 *  - reports BY them: kept (the person they reported may still be dangerous), but the reporter id is
 *    replaced by a pseudonym. Same pseudonym for all their reports, so "3 different reporters" still
 *    counts them once, and the open-report-per-pair index can't clash.
 *  - screenshots they took: deleted (nobody left to warn about)
 *  - screenshots taken OF them: kept for the other person's pattern count, with the pseudonym
 *  - blocks and their review status: deleted
 * Idempotent: a second run finds no documents with the real id any more.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final MongoTemplate mongo;

    public AccountDeletionService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Deterministic but not the real id; never matches a real user (name-based UUID, version 3). */
    static String pseudonym(UUID userId) {
        return UUID.nameUUIDFromBytes(("deleted-user:" + userId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    public void forget(UUID userId) {
        String id = userId.toString();
        String alias = pseudonym(userId);

        long reportsBy = mongo.updateMulti(Query.query(Criteria.where("reporterId").is(id)),
                Update.update("reporterId", alias), Report.class).getModifiedCount();
        long capturesTaken = mongo.remove(Query.query(Criteria.where("takerId").is(id)), CaptureRecord.class)
                .getDeletedCount();
        long capturesOf = mongo.updateMulti(Query.query(Criteria.where("otherUserId").is(id)),
                Update.update("otherUserId", alias), CaptureRecord.class).getModifiedCount();
        long blocks = mongo.remove(Query.query(new Criteria().orOperator(
                Criteria.where("blockerId").is(id), Criteria.where("blockedId").is(id))), Block.class).getDeletedCount();
        mongo.remove(Query.query(Criteria.where("_id").is(id)), UserSafetyStatus.class);

        log.info("Deleted user {}: {} report(s) by them anonymised, {} block(s) and {} capture record(s) removed, "
                + "{} capture record(s) anonymised; reports about them kept as evidence",
                userId, reportsBy, blocks, capturesTaken, capturesOf);
    }
}
