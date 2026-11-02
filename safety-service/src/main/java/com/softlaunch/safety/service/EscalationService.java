package com.softlaunch.safety.service;

import com.softlaunch.safety.config.SafetyProperties;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.ReportPriority;
import com.softlaunch.safety.model.ReportStatus;
import com.softlaunch.safety.model.UserSafetyStatus;
import com.softlaunch.safety.repository.ReportRepository;
import com.softlaunch.safety.repository.UserSafetyStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Many DIFFERENT people reporting the same user is a strong signal (one angry ex is not).
 * We count distinct reporters, not reports.
 */
@Service
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final MongoTemplate mongoTemplate;
    private final ReportRepository reportRepository;
    private final UserSafetyStatusRepository statusRepository;
    private final SafetyProperties props;

    public EscalationService(MongoTemplate mongoTemplate, ReportRepository reportRepository,
                             UserSafetyStatusRepository statusRepository, SafetyProperties props) {
        this.mongoTemplate = mongoTemplate;
        this.reportRepository = reportRepository;
        this.statusRepository = statusRepository;
        this.props = props;
    }

    public long distinctReporters(UUID userId) {
        Instant since = Instant.now().minus(props.escalationWindow());
        Query query = Query.query(Criteria.where("reportedUserId").is(userId.toString()).and("createdAt").gte(since));
        return mongoTemplate.findDistinct(query, "reporterId", Report.class, String.class).size();
    }

    /** Called after a report is saved. */
    public void evaluate(Report newReport) {
        UUID userId = newReport.getReportedUserId();
        long reporters = distinctReporters(userId);
        boolean urgent = newReport.getPriority() == ReportPriority.URGENT;

        if (reporters < props.escalationReporters() && !urgent) {
            return;
        }

        if (reporters >= props.escalationReporters()) {
            // lift ALL of this user's open reports, so moderators see the whole picture together
            List<Report> open = reportRepository.findByReportedUserIdAndStatus(userId.toString(), ReportStatus.OPEN);
            open.forEach(r -> r.escalateTo(ReportPriority.HIGH));
            reportRepository.saveAll(open);
        }

        String reason = urgent
                ? "Urgent report: " + newReport.getReason()
                : reporters + " different users reported this person";
        UserSafetyStatus status = statusRepository.findById(userId.toString())
                .orElseGet(() -> UserSafetyStatus.clean(userId));
        if (status.putUnderReview(reason)) {
            statusRepository.save(status);
            log.warn("User {} is now UNDER REVIEW: {}", userId, reason);
        }
    }
}
