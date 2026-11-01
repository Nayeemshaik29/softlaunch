package com.softlaunch.chat.messaging;

import com.softlaunch.chat.event.MatchRemovedEvent;
import com.softlaunch.chat.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class MatchRemovedListener {

    private static final Logger log = LoggerFactory.getLogger(MatchRemovedListener.class);

    private final MessageRepository messageRepository;
    private final JsonMapper jsonMapper;

    public MatchRemovedListener(MessageRepository messageRepository, JsonMapper jsonMapper) {
        this.messageRepository = messageRepository;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "match.removed")
    public void onMatchRemoved(String payload) {
        MatchRemovedEvent event = jsonMapper.readValue(payload, MatchRemovedEvent.class);
        if (event.matchId() == null) {
            log.warn("Ignoring match.removed without matchId: {}", payload);
            return;
        }
        long deleted = messageRepository.deleteByMatchId(event.matchId().toString());
        log.info("Match {} removed, deleted {} chat message(s)", event.matchId(), deleted);
    }
}
