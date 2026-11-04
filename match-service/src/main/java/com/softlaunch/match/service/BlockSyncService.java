package com.softlaunch.match.service;

import com.softlaunch.match.event.BlockChangedEvent;
import com.softlaunch.match.event.MatchRemovedEvent;
import com.softlaunch.match.model.Match;
import com.softlaunch.match.model.UserBlock;
import com.softlaunch.match.repository.MatchRepository;
import com.softlaunch.match.repository.SwipeRepository;
import com.softlaunch.match.repository.UserBlockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BlockSyncService {

    private static final Logger log = LoggerFactory.getLogger(BlockSyncService.class);

    private final UserBlockRepository blockRepository;
    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BlockSyncService(UserBlockRepository blockRepository, MatchRepository matchRepository,
                            SwipeRepository swipeRepository, ApplicationEventPublisher eventPublisher) {
        this.blockRepository = blockRepository;
        this.matchRepository = matchRepository;
        this.swipeRepository = swipeRepository;
        this.eventPublisher = eventPublisher;
    }

    /** Idempotent: replaying the same event leaves the same end state. */
    @Transactional
    public void apply(BlockChangedEvent event) {
        UUID blocker = event.blockerId();
        UUID blocked = event.blockedId();

        if (event.active()) {
            if (!blockRepository.existsByBlockerIdAndBlockedId(blocker, blocked)) {
                blockRepository.save(new UserBlock(blocker, blocked));
            }
            int swipes = swipeRepository.deleteBetween(blocker, blocked);
            Match pair = Match.between(blocker, blocked);
            matchRepository.findByUserAIdAndUserBId(pair.getUserAId(), pair.getUserBId()).ifPresent(match -> {
                matchRepository.delete(match);
                // same event as a normal unmatch -> chat + notifications clean themselves up.
                // To the blocked person this looks exactly like an unmatch.
                eventPublisher.publishEvent(MatchRemovedEvent.of(
                        match.getId(), match.getUserAId(), match.getUserBId(), blocker));
                log.info("Block {} -> {}: removed match {}", blocker, blocked, match.getId());
            });
            log.info("Block {} -> {} applied ({} swipe(s) cleared)", blocker, blocked, swipes);
        } else {
            blockRepository.deleteDirection(blocker, blocked);
            // fresh start: clear swipes so they can find each other again (only if no block remains)
            if (!blockRepository.isBlocked(blocker, blocked)) {
                swipeRepository.deleteBetween(blocker, blocked);
            }
            log.info("Unblock {} -> {} applied", blocker, blocked);
        }
    }
}
