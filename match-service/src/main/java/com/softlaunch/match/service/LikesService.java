package com.softlaunch.match.service;

import com.softlaunch.match.exception.PlusRequiredException;
import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;
import com.softlaunch.match.repository.SwipeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Plus perks that live in match-service: "who liked you" and Rewind. */
@Service
public class LikesService {

    private static final Duration REWIND_WINDOW = Duration.ofHours(24);

    private final SwipeRepository swipes;
    private final PlusChecker plus;

    public LikesService(SwipeRepository swipes, PlusChecker plus) {
        this.swipes = swipes;
        this.plus = plus;
    }

    public record Like(UUID userId, boolean superLike, Instant likedAt) {
    }

    /** Everyone sees HOW MANY people liked them; only Plus sees WHO. */
    public record Likes(int count, boolean plus, List<Like> likes) {
    }

    @Transactional(readOnly = true)
    public Likes likesOf(UUID me) {
        List<Swipe> pending = swipes.findPendingLikesOf(me);
        boolean isPlus = plus.isPlus(me);
        List<Like> who = isPlus
                ? pending.stream().map(s -> new Like(s.getSwiperId(), s.getDirection() == SwipeDirection.SUPER_LIKE, s.getCreatedAt())).toList()
                : List.of();
        return new Likes(pending.size(), isPlus, who);
    }

    public record Rewound(UUID targetUserId) {
    }

    /** Undo my last PASS (within 24 h), so that person can appear again. Likes can't be undone (they may have matched). */
    @Transactional
    public Rewound rewind(UUID me) {
        if (!plus.isPlus(me)) throw new PlusRequiredException("Rewind");
        Swipe last = swipes.findFirstBySwiperIdOrderByCreatedAtDesc(me)
                .filter(s -> s.getDirection() == SwipeDirection.PASS)
                .filter(s -> s.getCreatedAt().isAfter(Instant.now().minus(REWIND_WINDOW)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Nothing to undo"));
        swipes.delete(last);
        return new Rewound(last.getTargetId());
    }
}
