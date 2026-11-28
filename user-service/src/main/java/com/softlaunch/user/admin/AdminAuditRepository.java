package com.softlaunch.user.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminAuditRepository extends JpaRepository<AdminAudit, UUID> {
    List<AdminAudit> findTop100ByOrderByAtDesc();
}
