package com.softlaunch.safety.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "reports")
public class Report {

    /** reporterId used for reports the system files on its own (e.g. screenshot pattern). */
    public static final UUID SYSTEM_REPORTER = new UUID(0L, 0L);

    @Id
    private String id;

    // ids stored as Strings: readable in mongosh and no UUID-encoding surprises (same as chat-service)
    private String reporterId;
    private String reportedUserId;
    private ReportReason reason;
    private String details;
    private String matchId;

    private EvidenceStatus evidenceStatus;
    private List<EvidenceMessage> evidence = new ArrayList<>();

    private ReportStatus status;
    private ReportPriority priority;
    private int priorityRank;

    private String moderatorNote;
    private Instant createdAt;
    private Instant reviewedAt;
    private Instant expiresAt;          // TTL index: Mongo deletes the report after the retention period

    protected Report() { }

    public static Report open(UUID reporterId, UUID reportedUserId, ReportReason reason, String details,
                              UUID matchId, Instant retentionUntil) {
        Report r = new Report();
        r.reporterId = reporterId.toString();
        r.reportedUserId = reportedUserId.toString();
        r.reason = reason;
        r.details = details;
        r.matchId = matchId == null ? null : matchId.toString();
        r.evidenceStatus = EvidenceStatus.NONE;
        r.status = ReportStatus.OPEN;
        r.setPriority(reason.isUrgent() ? ReportPriority.URGENT : ReportPriority.NORMAL);
        r.createdAt = Instant.now();
        r.expiresAt = retentionUntil;
        return r;
    }

    public void attachEvidence(List<EvidenceMessage> messages) {
        this.evidence = new ArrayList<>(messages);
        this.evidenceStatus = EvidenceStatus.CAPTURED;
    }

    public void evidenceUnavailable() {
        this.evidenceStatus = EvidenceStatus.UNAVAILABLE;
    }

    /** Raise priority only (URGENT never drops to HIGH). */
    public void escalateTo(ReportPriority newPriority) {
        if (newPriority.rank() < priority.rank()) {
            setPriority(newPriority);
        }
    }

    public void review(ReportStatus outcome, String note) {
        if (outcome == ReportStatus.OPEN) {
            throw new IllegalArgumentException("A review must RESOLVE or DISMISS the report");
        }
        this.status = outcome;
        this.moderatorNote = note;
        this.reviewedAt = Instant.now();
    }

    private void setPriority(ReportPriority p) {
        this.priority = p;
        this.priorityRank = p.rank();
    }

    public String getId() { return id; }
    public UUID getReporterId() { return UUID.fromString(reporterId); }
    public UUID getReportedUserId() { return UUID.fromString(reportedUserId); }
    public ReportReason getReason() { return reason; }
    public String getDetails() { return details; }
    public UUID getMatchId() { return matchId == null ? null : UUID.fromString(matchId); }
    public EvidenceStatus getEvidenceStatus() { return evidenceStatus; }
    public List<EvidenceMessage> getEvidence() { return List.copyOf(evidence); }
    public ReportStatus getStatus() { return status; }
    public ReportPriority getPriority() { return priority; }
    public String getModeratorNote() { return moderatorNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
