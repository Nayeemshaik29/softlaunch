package com.softlaunch.safety.service;

import com.softlaunch.safety.client.UserClient;
import com.softlaunch.safety.dto.BlockRequest;
import com.softlaunch.safety.dto.BlockResponse;
import com.softlaunch.safety.dto.CreateReportRequest;
import com.softlaunch.safety.event.BlockChangedEvent;
import com.softlaunch.safety.model.Block;
import com.softlaunch.safety.outbox.PendingEvents;
import com.softlaunch.safety.repository.BlockRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class BlockService {

    private static final Logger log = LoggerFactory.getLogger(BlockService.class);

    private final BlockRepository blockRepository;
    private final ReportService reportService;
    private final UserClient userClient;
    private final PendingEvents pendingEvents;

    public BlockService(BlockRepository blockRepository, ReportService reportService, UserClient userClient,
                        PendingEvents pendingEvents) {
        this.blockRepository = blockRepository;
        this.reportService = reportService;
        this.userClient = userClient;
        this.pendingEvents = pendingEvents;
    }

    public BlockResponse block(UUID me, BlockRequest req) {
        UUID target = req.blockedUserId();
        if (me.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't block yourself");
        }
        Block block = blockRepository.findByBlockerIdAndBlockedId(me.toString(), target.toString()).orElse(null);
        if (block != null && block.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You've already blocked this person");
        }
        if (block == null) {
            requireUserExists(target);
            block = Block.create(me, target);
        }

        // 1) Report FIRST: blocking deletes the chat, so evidence must be copied before that.
        Boolean reportFiled = null;
        if (req.report() != null) {
            reportFiled = fileReport(me, target, req.report());
        }

        // 2) Block: state + its event saved in ONE document write (embedded outbox).
        block.activate(pendingEvents.of(BlockChangedEvent.of(me, target, true)));
        try {
            block = blockRepository.save(block);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You've already blocked this person");
        }
        log.info("User {} blocked {} (report filed: {})", me, target, reportFiled);
        return BlockResponse.from(block, reportFiled);
    }

    public void unblock(UUID me, UUID target) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(me.toString(), target.toString())
                .filter(Block::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You haven't blocked this person"));
        block.deactivate(pendingEvents.of(BlockChangedEvent.of(me, target, false)));
        blockRepository.save(block);
        log.info("User {} unblocked {}", me, target);
    }

    public List<BlockResponse> myBlocks(UUID me) {
        return blockRepository.findByBlockerIdAndActiveTrueOrderByBlockedAtDesc(me.toString()).stream()
                .map(b -> BlockResponse.from(b, null))
                .toList();
    }

    /** A failed report must never stop the block: safety first. */
    private boolean fileReport(UUID me, UUID target, BlockRequest.ReportPart part) {
        try {
            reportService.create(me, new CreateReportRequest(target, part.reason(), part.details(), part.matchId()));
            return true;
        } catch (ResponseStatusException e) {
            log.info("Block of {} by {}: report not filed ({}), blocking anyway", target, me, e.getReason());
            return false;
        }
    }

    private void requireUserExists(UUID userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Blocking is temporarily unavailable");
        }
    }
}
