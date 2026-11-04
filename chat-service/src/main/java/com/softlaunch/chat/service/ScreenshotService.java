package com.softlaunch.chat.service;

import com.softlaunch.chat.client.MatchSummary;
import com.softlaunch.chat.dto.CaptureAlert;
import com.softlaunch.chat.dto.MessageResponse;
import com.softlaunch.chat.dto.ScreenshotResult;
import com.softlaunch.chat.event.MessageEventPublisher;
import com.softlaunch.chat.event.ScreenshotTakenEvent;
import com.softlaunch.chat.model.CaptureKind;
import com.softlaunch.chat.model.Message;
import com.softlaunch.chat.model.MessageType;
import com.softlaunch.chat.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The backend half of screenshot protection. The phone detects the capture (iOS) and calls us;
 * we warn the other person live, leave a line in the chat, and tell safety-service.
 */
@Service
public class ScreenshotService {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotService.class);
    private static final Duration DEBOUNCE = Duration.ofSeconds(10);

    private final ParticipantGuard participantGuard;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageEventPublisher eventPublisher;
    private final StringRedisTemplate redis;
    private final Duration messageTtl;

    public ScreenshotService(ParticipantGuard participantGuard, MessageRepository messageRepository,
                             SimpMessagingTemplate messagingTemplate, MessageEventPublisher eventPublisher,
                             StringRedisTemplate redis, @Value("${softlaunch.chat.message-ttl}") Duration messageTtl) {
        this.participantGuard = participantGuard;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.redis = redis;
        this.messageTtl = messageTtl;
    }

    public ScreenshotResult captured(UUID me, UUID matchId, CaptureKind kind) {
        MatchSummary match = participantGuard.requireParticipant(me, matchId);   // 404 if not my chat

        // Debounce: SET key NX EX 10 -> only the first capture in 10s gets through
        Boolean first = redis.opsForValue().setIfAbsent("capture:" + me + ":" + matchId, kind.name(), DEBOUNCE);
        if (!Boolean.TRUE.equals(first)) {
            return ScreenshotResult.DEBOUNCED;
        }

        Instant now = Instant.now();
        String topic = "/topic/chats/" + matchId;

        // 1) live alert (works even if Kafka is down)
        messagingTemplate.convertAndSend(topic + "/alerts", CaptureAlert.of(kind, me.toString(), now));

        // 2) system line in the chat, so someone who was offline still sees it
        Message line = messageRepository.save(new Message(matchId.toString(), me.toString(), kind.systemText(),
                now, now.plus(messageTtl), MessageType.SYSTEM));
        messagingTemplate.convertAndSend(topic, MessageResponse.from(line));

        // 3) tell safety-service
        eventPublisher.publish(ScreenshotTakenEvent.of(matchId, me, match.otherThan(me), kind, now));
        log.info("{} in match {} by {}: other person alerted", kind, matchId, me);
        return ScreenshotResult.ALERTED;
    }
}
