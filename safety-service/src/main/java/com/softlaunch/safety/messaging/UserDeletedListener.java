package com.softlaunch.safety.messaging;

import com.softlaunch.safety.exception.InvalidEventException;
import com.softlaunch.safety.service.AccountDeletionService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class UserDeletedListener {

    private final AccountDeletionService deletion;
    private final JsonMapper json;

    public UserDeletedListener(AccountDeletionService deletion, JsonMapper json) {
        this.deletion = deletion;
        this.json = json;
    }

    @KafkaListener(topics = "user.deleted")
    public void onUserDeleted(String payload) {
        JsonNode userId = json.readTree(payload).get("userId");
        if (userId == null || userId.isNull()) {
            throw new InvalidEventException("user.deleted without userId");
        }
        deletion.forget(UUID.fromString(userId.asString()));
    }
}
