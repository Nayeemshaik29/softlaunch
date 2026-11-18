package com.softlaunch.match.service;

import com.softlaunch.match.event.MatchRemovedEvent;
import com.softlaunch.match.model.Match;
import com.softlaunch.match.repository.MatchRepository;
import com.softlaunch.match.repository.SwipeRepository;
import com.softlaunch.match.repository.UserBlockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * user.deleted -> remove everything match-service knows about that person.
 * Each match is removed like a normal unmatch (match.removed, sent after commit), so chat deletes the
 * conversation, notification deletes the match notifications and ai-service drops cached openers:
 * one cleanup path, already tested, instead of a second one.
 * Idempotent: a second delivery finds nothing left.
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final MatchRepository matches;
    private final SwipeRepository swipes;
    private final UserBlockRepository blocks;
    private final ApplicationEventPublisher events;

    public AccountDeletionService(MatchRepository matches, SwipeRepository swipes, UserBlockRepository blocks,
                                  ApplicationEventPublisher events) {
        this.matches = matches;
        this.swipes = swipes;
        this.blocks = blocks;
        this.events = events;
    }

    @Transactional
    public void forget(UUID userId) {
        List<Match> theirs = matches.findByUserAIdOrUserBIdOrderByCreatedAtDesc(userId, userId);
        for (Match match : theirs) {
            matches.delete(match);
            events.publishEvent(MatchRemovedEvent.of(match.getId(), match.getUserAId(), match.getUserBId(), userId));
        }
        int swipeCount = swipes.deleteAllInvolving(userId);
        int blockCount = blocks.deleteAllInvolving(userId);
        log.info("Deleted user {}: removed {} match(es), {} swipe(s), {} block(s)",
                userId, theirs.size(), swipeCount, blockCount);
    }
}
