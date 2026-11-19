package com.softlaunch.match.service;

import com.softlaunch.match.exception.PlusRequiredException;
import com.softlaunch.match.model.Swipe;
import com.softlaunch.match.model.SwipeDirection;
import com.softlaunch.match.repository.SwipeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LikesServiceTest {

    private final SwipeRepository swipes = mock(SwipeRepository.class);
    private final PlusChecker plus = mock(PlusChecker.class);
    private final LikesService service = new LikesService(swipes, plus);
    private final UUID me = UUID.randomUUID();

    @Test
    void freeUsersSeeHowManyButNotWho() {
        when(swipes.findPendingLikesOf(me)).thenReturn(List.of(new Swipe(UUID.randomUUID(), me, SwipeDirection.LIKE),
                new Swipe(UUID.randomUUID(), me, SwipeDirection.SUPER_LIKE)));
        var likes = service.likesOf(me);
        assertThat(likes.count()).isEqualTo(2);
        assertThat(likes.likes()).isEmpty();
        assertThat(likes.plus()).isFalse();
    }

    @Test
    void plusSeesWhoIncludingSuperLikes() {
        when(plus.isPlus(me)).thenReturn(true);
        UUID fan = UUID.randomUUID();
        when(swipes.findPendingLikesOf(me)).thenReturn(List.of(new Swipe(fan, me, SwipeDirection.SUPER_LIKE)));
        var likes = service.likesOf(me);
        assertThat(likes.likes()).singleElement().satisfies(l -> {
            assertThat(l.userId()).isEqualTo(fan);
            assertThat(l.superLike()).isTrue();
        });
    }

    @Test
    void rewindIsPlusOnly() {
        assertThatThrownBy(() -> service.rewind(me)).isInstanceOf(PlusRequiredException.class);
        verify(swipes, never()).delete(any());
    }

    @Test
    void rewindUndoesTheLastPassButNeverALike() {
        when(plus.isPlus(me)).thenReturn(true);
        UUID them = UUID.randomUUID();
        Swipe pass = new Swipe(me, them, SwipeDirection.PASS);
        when(swipes.findFirstBySwiperIdOrderByCreatedAtDesc(me)).thenReturn(Optional.of(pass));
        assertThat(service.rewind(me).targetUserId()).isEqualTo(them);
        verify(swipes).delete(pass);

        when(swipes.findFirstBySwiperIdOrderByCreatedAtDesc(me)).thenReturn(Optional.of(new Swipe(me, them, SwipeDirection.LIKE)));
        assertThatThrownBy(() -> service.rewind(me)).isInstanceOf(ResponseStatusException.class);
    }
}
