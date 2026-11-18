package com.softlaunch.chat.messaging;

import com.softlaunch.chat.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Messages need nothing here: match-service removes every match of the deleted user and sends
 * match.removed for each, which MatchRemovedListener already turns into "delete that chat".
 * Only the presence keys are per user.
 */
@Component
public class UserDeletedListener {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedListener.class);

    private final PresenceService presence;
    private final JsonMapper json;

    public UserDeletedListener(PresenceService presence, JsonMapper json) {
        this.presence = presence;
        this.json = json;
    }

    @KafkaListener(topics = "user.deleted")
    public void onUserDeleted(String payload) {
        JsonNode userId = json.readTree(payload).get("userId");
        if (userId == null || userId.isNull()) {
            log.warn("Ignoring user.deleted without userId");
            return;
        }
        presence.forget(userId.asString());
        log.info("Deleted user {}: presence removed (chats go via match.removed)", userId.asString());
    }
}
