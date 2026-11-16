package com.softlaunch.safety.service;

import com.softlaunch.safety.config.SafetyProperties;
import com.softlaunch.safety.event.ScamSuspectedEvent;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.ReportPriority;
import com.softlaunch.safety.model.ReportReason;
import com.softlaunch.safety.observability.SafetyMetrics;
import com.softlaunch.safety.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Turns an automatic scam suspicion into a HIGH-priority report for a moderator, with the chat copied
 * as evidence. Filed by Report.SCAM_DETECTOR (not SYSTEM_REPORTER), so it never collides with an open
 * screenshot-pattern report about the same person (the DB allows one OPEN report per reporter + target).
 * One open scam report per person is enough: later hits while it is open are ignored.
 * Never blocks or bans: a human decides.
 */
@Service
public class ScamMonitor {

    private static final Logger log = LoggerFactory.getLogger(ScamMonitor.class);

    private final ReportRepository reportRepository;
    private final EvidenceCollector evidenceCollector;
    private final SafetyProperties props;
    private final SafetyMetrics metrics;

    public ScamMonitor(ReportRepository reportRepository, EvidenceCollector evidenceCollector, SafetyProperties props,
                       SafetyMetrics metrics) {
        this.reportRepository = reportRepository;
        this.evidenceCollector = evidenceCollector;
        this.props = props;
        this.metrics = metrics;
    }

    public void record(ScamSuspectedEvent e) {
        String details = "Automatic scam check: " + e.category().toLowerCase(Locale.ROOT).replace('_', ' ')
                + switch (e.source()) {
                    case "ai" -> " (AI confidence " + Math.round(e.confidence() * 100) + "%)";
                    case "rules_override" -> " (AI said not a scam, but the message tries to manipulate the automatic check)";
                    default -> " (rules only, AI was unavailable)";
                }
                + ", signals " + new TreeSet<>(e.signals() == null ? java.util.Set.of() : e.signals())
                + ", flagged message " + e.messageId();
        Report report = Report.open(Report.SCAM_DETECTOR, e.senderId(), ReportReason.SCAM, details, e.matchId(),
                Instant.now().plus(props.retention()));
        report.escalateTo(ReportPriority.HIGH);
        evidenceCollector.collect(report);   // copies the recent chat now: messages disappear after 24h
        try {
            reportRepository.save(report);
            metrics.reportFiled(ReportReason.SCAM, ReportPriority.HIGH);
            log.warn("Scam report filed against user {} ({}, source {})", e.senderId(), e.category(), e.source());
        } catch (DuplicateKeyException dup) {
            log.info("Scam report for user {} already open, ignoring new hit {}", e.senderId(), e.eventId());
        }
    }
}
