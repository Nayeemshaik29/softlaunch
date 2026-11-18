package com.softlaunch.user.session;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One refresh token. Only its SHA-256 is stored: a database leak can't be turned into logins.
 * familyId = one login session; every rotation stays in the same family, so a stolen token can kill the whole session.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_user", columnList = "userId"),
        @Index(name = "idx_refresh_family", columnList = "familyId"),
        @Index(name = "idx_refresh_expires", columnList = "expiresAt")
})
public class RefreshToken {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    protected RefreshToken() {
    }

    RefreshToken(UUID userId, UUID familyId, String tokenHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() { return userId; }
    public UUID getFamilyId() { return familyId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public boolean isRevoked() { return revokedAt != null; }

    void revoke(Instant now) {
        if (revokedAt == null) revokedAt = now;
    }
}
