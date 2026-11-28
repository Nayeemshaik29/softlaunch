package com.softlaunch.user.admin;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** One row per admin action or page view. Who looked at what, and when. Kept for accountability (DPDP). */
@Entity
@Table(name = "admin_audit", indexes = @Index(name = "idx_admin_audit_at", columnList = "at"))
public class AdminAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID adminId;

    @Column(nullable = false, length = 40)
    private String action;

    private UUID targetUserId;

    @Column(nullable = false)
    private Instant at;

    protected AdminAudit() {
    }

    public AdminAudit(UUID adminId, String action, UUID targetUserId) {
        this.adminId = adminId;
        this.action = action;
        this.targetUserId = targetUserId;
        this.at = Instant.now();
    }

    public UUID getAdminId() { return adminId; }
    public String getAction() { return action; }
    public UUID getTargetUserId() { return targetUserId; }
    public Instant getAt() { return at; }
}
