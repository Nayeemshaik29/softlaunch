package com.softlaunch.safety.service;

import com.softlaunch.safety.config.SafetyProperties;
import com.softlaunch.safety.event.ScreenshotTakenEvent;
import com.softlaunch.safety.model.CaptureRecord;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.ReportPriority;
import com.softlaunch.safety.model.ReportReason;
import com.softlaunch.safety.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * One screenshot is a SIGNAL, not a verdict (people save sweet messages).
 * Capturing chats of many DIFFERENT people is a pattern -> a system report for a human moderator. No auto-ban.
 */
@Service
public class CaptureMonitor {

    private static final Logger log = LoggerFactory.getLogger(CaptureMonitor.class);

    private final MongoTemplate mongoTemplate;
    private final ReportRepository reportRepository;
    private final SafetyProperties props;

    public CaptureMonitor(MongoTemplate mongoTemplate, ReportRepository reportRepository, SafetyProperties props) {
        this.mongoTemplate = mongoTemplate;
        this.reportRepository = reportRepository;
        this.props = props;
    }

    public void record(ScreenshotTakenEvent e) {
        try {
            mongoTemplate.insert(new CaptureRecord(e.eventId(), e.takerId(), e.otherUserId(), e.matchId(), e.kind(),
                    e.occurredAt(), e.occurredAt().plus(props.screenshotRetention())));
        } catch (DuplicateKeyException dup) {
            log.info("Capture event {} already recorded, skipping", e.eventId());   // idempotent
            return;
        }

        long people = distinctPeopleCaptured(e.takerId());
        log.info("{} by {} recorded ({} different people's chats in {})", e.kind(), e.takerId(), people,
                props.screenshotWindow());
        if (people >= props.screenshotPeople()) {
            fileSystemReport(e.takerId(), people);
        }
    }

    private long distinctPeopleCaptured(UUID takerId) {
        Instant since = Instant.now().minus(props.screenshotWindow());
        Query q = Query.query(Criteria.where("takerId").is(takerId.toString()).and("occurredAt").gte(since));
        return mongoTemplate.findDistinct(q, "otherUserId", CaptureRecord.class, String.class).size();
    }

    private void fileSystemReport(UUID takerId, long people) {
        Report report = Report.open(Report.SYSTEM_REPORTER, takerId, ReportReason.SCREENSHOT_PATTERN,
                "Captured chats of " + people + " different people in " + props.screenshotWindow().toDays() + " days",
                null, Instant.now().plus(props.retention()));
        report.escalateTo(ReportPriority.HIGH);
        try {
            reportRepository.save(report);
            log.warn("System report filed: user {} captured chats of {} different people", takerId, people);
        } catch (DuplicateKeyException dup) {
            // an open system report already exists for this user: one is enough
        }
    }
}
