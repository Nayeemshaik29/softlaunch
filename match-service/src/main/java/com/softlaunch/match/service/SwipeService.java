package com.softlaunch.match.service;

import com.softlaunch.match.dto.SwipeRequest;
import com.softlaunch.match.dto.SwipeResponse;
import com.softlaunch.match.exception.AlreadySwipedException;
import com.softlaunch.match.exception.CannotSwipeSelfException;
import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.repository.SwipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SwipeService {

    private final SwipeRepository swipeRepository;

    public SwipeService(SwipeRepository swipeRepository) {
        this.swipeRepository = swipeRepository;
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

        Swipe saved = swipeRepository.save(new Swipe(swiperId, targetId, request.direction()));
        return SwipeResponse.from(saved);
    }
}