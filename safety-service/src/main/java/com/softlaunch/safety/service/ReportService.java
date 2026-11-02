package com.softlaunch.safety.service;

import com.softlaunch.safety.client.UserClient;
import com.softlaunch.safety.config.SafetyProperties;
import com.softlaunch.safety.dto.CreateReportRequest;
import com.softlaunch.safety.dto.MyReportResponse;
import com.softlaunch.safety.dto.ReportReceipt;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.repository.ReportRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final UserClient userClient;
    private final EvidenceCollector evidenceCollector;
    private final EscalationService escalationService;
    private final ReportRateLimiter rateLimiter;
    private final SafetyProperties props;

    public ReportService(ReportRepository reportRepository, UserClient userClient, EvidenceCollector evidenceCollector,
                         EscalationService escalationService, ReportRateLimiter rateLimiter, SafetyProperties props) {
        this.reportRepository = reportRepository;
        this.userClient = userClient;
        this.evidenceCollector = evidenceCollector;
        this.escalationService = escalationService;
        this.rateLimiter = rateLimiter;
        this.props = props;
    }

    public ReportReceipt create(UUID reporterId, CreateReportRequest req) {
        if (reporterId.equals(req.reportedUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't report yourself");
        }
        rateLimiter.check(reporterId);
        requireUserExists(req.reportedUserId());

        Report report = Report.open(reporterId, req.reportedUserId(), req.reason(), req.details(), req.matchId(),
                Instant.now().plus(props.retention()));
        evidenceCollector.collect(report);

        try {
            report = reportRepository.save(report);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You've already reported this person. Our team is reviewing it.");
        }
        log.info("Report {} created: {} reported {} for {} (priority {}, evidence {} / {} msgs)", report.getId(),
                reporterId, req.reportedUserId(), req.reason(), report.getPriority(), report.getEvidenceStatus(),
                report.getEvidence().size());

        escalationService.evaluate(report);
        return ReportReceipt.received(report.getId());
    }

    public List<MyReportResponse> mine(UUID reporterId) {
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId.toString()).stream()
                .map(MyReportResponse::from)
                .toList();
    }

    private void requireUserExists(UUID userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Reporting is temporarily unavailable");
        }
    }
}
