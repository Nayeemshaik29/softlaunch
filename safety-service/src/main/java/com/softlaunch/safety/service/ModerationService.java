package com.softlaunch.safety.service;

import com.softlaunch.safety.dto.ModeratorReportResponse;
import com.softlaunch.safety.dto.ReviewRequest;
import com.softlaunch.safety.dto.UserSafetyResponse;
import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.ReportStatus;
import com.softlaunch.safety.model.UserSafetyStatus;
import com.softlaunch.safety.repository.ReportRepository;
import com.softlaunch.safety.repository.UserSafetyStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    private final ReportRepository reportRepository;
    private final UserSafetyStatusRepository statusRepository;
    private final EscalationService escalationService;

    public ModerationService(ReportRepository reportRepository, UserSafetyStatusRepository statusRepository,
                             EscalationService escalationService) {
        this.reportRepository = reportRepository;
        this.statusRepository = statusRepository;
        this.escalationService = escalationService;
    }

    /** The moderator queue: URGENT first, then HIGH, then NORMAL; oldest first inside each. */
    public List<ModeratorReportResponse> queue(ReportStatus status, int limit) {
        return reportRepository.findByStatusOrderByPriorityRankAscCreatedAtAsc(status,
                        PageRequest.of(0, Math.min(Math.max(limit, 1), 100))).stream()
                .map(ModeratorReportResponse::from)
                .toList();
    }

    public ModeratorReportResponse review(String reportId, ReviewRequest req) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        if (report.getStatus() != ReportStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report was already reviewed");
        }
        try {
            report.review(req.status(), req.moderatorNote());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        reportRepository.save(report);
        log.info("Report {} reviewed: {}", reportId, req.status());

        // nothing left open for this person -> they are no longer under review
        UUID userId = report.getReportedUserId();
        if (reportRepository.countByReportedUserIdAndStatus(userId.toString(), ReportStatus.OPEN) == 0) {
            statusRepository.findById(userId.toString()).filter(UserSafetyStatus::isUnderReview).ifPresent(s -> {
                s.clearReview();
                statusRepository.save(s);
                log.info("User {} is no longer under review", userId);
            });
        }
        return ModeratorReportResponse.from(report);
    }

    public UserSafetyResponse userStatus(UUID userId) {
        UserSafetyStatus s = statusRepository.findById(userId.toString()).orElseGet(() -> UserSafetyStatus.clean(userId));
        return new UserSafetyResponse(userId, s.isUnderReview(), s.getReviewReason(), s.getUnderReviewSince(),
                reportRepository.countByReportedUserIdAndStatus(userId.toString(), ReportStatus.OPEN),
                escalationService.distinctReporters(userId));
    }
}
