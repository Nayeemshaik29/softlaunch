package com.softlaunch.match.service;

import com.softlaunch.match.dto.SwipeRequest;
import com.softlaunch.match.dto.SwipeResponse;
import com.softlaunch.match.event.MatchCreatedEvent;
import com.softlaunch.match.event.SwipeRecordedEvent; // 🆕
import com.softlaunch.match.exception.AlreadySwipedException;
import com.softlaunch.match.exception.CannotSwipeSelfException;
import com.softlaunch.match.model.Match;
import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;
import com.softlaunch.match.repository.MatchRepository;
import com.softlaunch.match.repository.SwipeRepository;
import com.softlaunch.match.repository.UserBlockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class SwipeService {

    private static final Set<SwipeDirection> POSITIVE = EnumSet.of(SwipeDirection.LIKE, SwipeDirection.SUPER_LIKE);

    private final ApplicationEventPublisher eventPublisher;
    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final SwipeQuotaService swipeQuotaService;
    private final UserVerifier userVerifier;
    private final UserBlockRepository userBlockRepository;
    private final com.softlaunch.match.activity.ActivityRecorder activity;

    public SwipeService(ApplicationEventPublisher eventPublisher, SwipeRepository swipeRepository,
                        MatchRepository matchRepository,
                        SwipeQuotaService swipeQuotaService, UserVerifier userVerifier,
                        UserBlockRepository userBlockRepository,
                        com.softlaunch.match.activity.ActivityRecorder activity) {
        this.activity = activity;
        this.userBlockRepository = userBlockRepository;
        this.eventPublisher = eventPublisher;
        this.swipeRepository = swipeRepository;
        this.matchRepository = matchRepository;
        this.swipeQuotaService = swipeQuotaService;
        this.userVerifier = userVerifier;
    }

    @Transactional
    public SwipeResponse swipe(UUID swiperId, SwipeRequest request) {
        UUID targetId = request.targetUserId();

        if (swiperId.equals(targetId)) {
            throw new CannotSwipeSelfException();
        }
        if (swipeRepository.existsBySwiperIdAndTargetId(swiperId, targetId)) {
            throw new AlreadySwipedException(targetId);
        }
        userVerifier.requireExists(targetId);
        boolean superLike = request.direction() == SwipeDirection.SUPER_LIKE;
        if (superLike) swipeQuotaService.consumeSuperLike(swiperId);
        try {
            swipeQuotaService.consume(swiperId);
        } catch (RuntimeException e) {
            if (superLike) swipeQuotaService.refundSuperLike(swiperId);   // the swipe didn't happen: give it back
            throw e;
        }

        Swipe saved = swipeRepository.save(new Swipe(swiperId, targetId, request.direction()));
        eventPublisher.publishEvent(
                SwipeRecordedEvent.of(saved.getId(), swiperId, targetId, request.direction().name())); // 🆕

        activity.count(swiperId, "swipes", 1);
        if (POSITIVE.contains(request.direction())) activity.count(swiperId, "likes", 1);

        UUID matchId = null;
        // Blocked pairs never match. We don't say why: the swipe just looks like "no match yet".
        if (POSITIVE.contains(request.direction()) && likedBack(targetId, swiperId)
                && !userBlockRepository.isBlocked(swiperId, targetId)) {
            matchId = createMatch(swiperId, targetId);
        }

        return SwipeResponse.from(saved, matchId);
    }

    private boolean likedBack(UUID otherUser, UUID me) {
        return swipeRepository.existsBySwiperIdAndTargetIdAndDirectionIn(otherUser, me, POSITIVE);
    }

    private UUID createMatch(UUID first, UUID second) {
        Match match = Match.between(first, second);
        if (matchRepository.existsByUserAIdAndUserBId(match.getUserAId(), match.getUserBId())) {
            return null;
        }
        Match saved = matchRepository.save(match);
        activity.count(first, "matches", 1);
        activity.count(second, "matches", 1);
        activity.feed(first, "match");

        eventPublisher.publishEvent(
                MatchCreatedEvent.of(saved.getId(), saved.getUserAId(), saved.getUserBId()));

        return saved.getId();
    }
}