package com.softlaunch.safety.repository;

import com.softlaunch.safety.model.Report;
import com.softlaunch.safety.model.ReportStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReportRepository extends MongoRepository<Report, String> {

    List<Report> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    List<Report> findByStatusOrderByPriorityRankAscCreatedAtAsc(ReportStatus status, Pageable page);

    List<Report> findByReportedUserIdAndStatus(String reportedUserId, ReportStatus status);

    long countByReportedUserIdAndStatus(String reportedUserId, ReportStatus status);
}
