package com.softlaunch.match.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** match-service's own copy of "A blocked B" (from the block.changed event). One row per direction. */
@Entity
@Table(name = "user_blocks",
        uniqueConstraints = @UniqueConstraint(name = "uk_block_direction", columnNames = {"blocker_id", "blocked_id"}))
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blocker_id", nullable = false)
    private UUID blockerId;

    @Column(name = "blocked_id", nullable = false)
    private UUID blockedId;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserBlock() {
    }

    public UserBlock(UUID blockerId, UUID blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = Instant.now();
    }
}
