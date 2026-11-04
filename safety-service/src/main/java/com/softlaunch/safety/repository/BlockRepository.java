package com.softlaunch.safety.repository;

import com.softlaunch.safety.model.Block;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends MongoRepository<Block, String> {
    Optional<Block> findByBlockerIdAndBlockedId(String blockerId, String blockedId);
    List<Block> findByBlockerIdAndActiveTrueOrderByBlockedAtDesc(String blockerId);
}
