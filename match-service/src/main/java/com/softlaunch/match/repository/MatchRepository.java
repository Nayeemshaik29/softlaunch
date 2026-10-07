package com.softlaunch.match.repository;

import com.softlaunch.match.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    boolean existsByUserAIdAndUserBId(UUID userAId, UUID userBId);

    List<Match> findByUserAIdOrUserBIdOrderByCreatedAtDesc(UUID userAId, UUID userBId);

}