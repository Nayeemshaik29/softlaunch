package com.softlaunch.safety.dto;

import com.softlaunch.safety.model.Block;

import java.time.Instant;
import java.util.UUID;

public record BlockResponse(UUID blockedUserId, Instant blockedAt, Boolean reportFiled) {
    public static BlockResponse from(Block b, Boolean reportFiled) {
        return new BlockResponse(b.getBlockedId(), b.getBlockedAt(), reportFiled);
    }
}
