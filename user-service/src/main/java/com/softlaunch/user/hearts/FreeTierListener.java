package com.softlaunch.user.hearts;

import com.softlaunch.user.event.ProfileUpdatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Gender changed or the Verified badge arrived: re-check free Plus right away (not only at the next login). */
@Component
public class FreeTierListener {

    private final FreeTier freeTier;

    public FreeTierListener(FreeTier freeTier) {
        this.freeTier = freeTier;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        freeTier.sync(event.userId());
    }
}
