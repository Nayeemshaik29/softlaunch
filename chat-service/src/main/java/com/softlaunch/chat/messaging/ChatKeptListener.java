package com.softlaunch.chat.messaging;

import com.softlaunch.chat.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

/** user-service charged 5 ❤ for "keep chat": extend that chat's messages (only if the payer is in it). */
@Component
public class ChatKeptListener {

    private static final Logger log = LoggerFactory.getLogger(ChatKeptListener.class);
    private final ChatService chats;
    private final JsonMapper json;

    public ChatKeptListener(ChatService chats, JsonMapper json) {
        this.chats = chats;
        this.json = json;
    }

    @KafkaListener(topics = "chat.kept")
    public void onKept(String payload) {
        JsonNode e = json.readTree(payload);
        UUID matchId = UUID.fromString(e.get("matchId").asString());
        UUID userId = UUID.fromString(e.get("userId").asString());
        int hours = Math.min(48, Math.max(1, e.path("hours").asInt(24)));
        try {
            int n = chats.keep(userId, matchId, hours);
            log.info("Chat {} kept {}h longer ({} messages)", matchId, hours, n);
        } catch (RuntimeException ex) {
            log.warn("Keep-chat for {} refused: {}", matchId, ex.getClass().getSimpleName());   // not a participant / match gone
        }
    }
}
