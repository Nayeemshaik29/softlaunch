package com.softlaunch.match.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_pair",
                columnNames = {"user_a_id", "user_b_id"})
)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_a_id", nullable = false)
    private UUID userAId;

    @Column(name = "user_b_id", nullable = false)
    private UUID userBId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Match() {
    }

    private Match(UUID userAId, UUID userBId) {
        this.userAId = userAId;
        this.userBId = userBId;
        this.createdAt = Instant.now();
    }

    public static Match between(UUID first, UUID second) {
        return first.compareTo(second) < 0
                ? new Match(first, second)
                : new Match(second, first);
    }

    public UUID otherUser(UUID me) {
        return me.equals(userAId) ? userBId : userAId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAId() {
        return userAId;
    }

    public UUID getUserBId() {
        return userBId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}