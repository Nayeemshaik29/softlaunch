package com.softlaunch.match.observability;

import com.softlaunch.match.event.MatchCreatedEvent;
import com.softlaunch.match.event.MatchRemovedEvent;
import com.softlaunch.match.event.SwipeRecordedEvent;
import com.softlaunch.match.model.SwipeDirection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.Map;

/**
 * Business counters for the product dashboard (swipes, matches, unmatches).
 * Counted AFTER_COMMIT from the same events the Kafka publisher uses, so a rolled-back swipe is never counted.
 * Tags only use small fixed sets (never user or match ids, that would create one time series per user).
 */
@Component
class BusinessMetrics {

    private final Map<SwipeDirection, Counter> swipes = new EnumMap<>(SwipeDirection.class);
    private final Counter matchesCreated;
    private final Counter matchesRemoved;

    BusinessMetrics(MeterRegistry registry) {
        // registered up front with value 0, so the series exist before the first swipe and rate() works right away
        for (SwipeDirection direction : SwipeDirection.values()) {
            swipes.put(direction, Counter.builder("softlaunch.swipes")
                    .description("Swipes recorded")
                    .tag("direction", direction.name())
                    .register(registry));
        }
        matchesCreated = Counter.builder("softlaunch.matches.new").description("New mutual matches (not \"matches.created\": Prometheus reserves the _created suffix)").register(registry);
        matchesRemoved = Counter.builder("softlaunch.matches.removed").description("Matches removed (unmatch or block)").register(registry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onSwipe(SwipeRecordedEvent event) {
        swipes.get(SwipeDirection.valueOf(event.direction())).increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onMatchCreated(MatchCreatedEvent event) {
        matchesCreated.increment();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onMatchRemoved(MatchRemovedEvent event) {
        matchesRemoved.increment();
    }
}
