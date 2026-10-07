package com.softlaunch.match.service;

import com.softlaunch.match.dto.SwipeRequest;
import com.softlaunch.match.dto.SwipeResponse;
import com.softlaunch.match.event.MatchCreatedEvent;
import com.softlaunch.match.exception.AlreadySwipedException;
import com.softlaunch.match.exception.CannotSwipeSelfException;
import com.softlaunch.match.model.Match;
import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;
import com.softlaunch.match.repository.MatchRepository;
import com.softlaunch.match.repository.SwipeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class SwipeService {
    private final ApplicationEventPublisher eventPublisher;
    private static final Set<SwipeDirection> POSITIVE = EnumSet.of(SwipeDirection.LIKE, SwipeDirection.SUPER_LIKE);

    private final SwipeRepository swipeRepository;
    private final MatchRepository matchRepository;
    private final SwipeQuotaService swipeQuotaService;
    private final UserVerifier userVerifier;

    public SwipeService(ApplicationEventPublisher eventPublisher, SwipeRepository swipeRepository,
                        MatchRepository matchRepository,
                        SwipeQuotaService swipeQuotaService, UserVerifier userVerifier) {
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
        swipeQuotaService.consume(swiperId);

        Swipe saved = swipeRepository.save(new Swipe(swiperId, targetId, request.direction()));

        UUID matchId = null;
        if (POSITIVE.contains(request.direction()) && likedBack(targetId, swiperId)) {
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

        eventPublisher.publishEvent(
                MatchCreatedEvent.of(saved.getId(), saved.getUserAId(), saved.getUserBId()));

        return saved.getId();
    }
}