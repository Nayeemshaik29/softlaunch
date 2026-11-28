package com.softlaunch.chat.service;

import com.softlaunch.chat.client.MatchSummary;
import com.softlaunch.chat.dto.MessageResponse;
import com.softlaunch.chat.event.MessageEventPublisher;
import com.softlaunch.chat.event.MessageScreeningEvent;
import com.softlaunch.chat.event.MessageSentEvent;
import com.softlaunch.chat.model.Message;
import com.softlaunch.chat.observability.ChatMetrics;
import com.softlaunch.chat.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final ParticipantGuard participantGuard;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final MessageEventPublisher messageEventPublisher;
    private final Duration messageTtl;
    private final ChatMetrics chatMetrics;

    private final com.softlaunch.chat.activity.ActivityRecorder activity;
    private final com.softlaunch.chat.client.UserHeartsClient hearts;

    public ChatService(MessageRepository messageRepository, ParticipantGuard participantGuard,
                       SimpMessagingTemplate messagingTemplate, PresenceService presenceService,
                       MessageEventPublisher messageEventPublisher,
                       @Value("${softlaunch.chat.message-ttl}") Duration messageTtl, ChatMetrics chatMetrics,
                       com.softlaunch.chat.activity.ActivityRecorder activity,
                       com.softlaunch.chat.client.UserHeartsClient hearts) {
        this.hearts = hearts;
        this.activity = activity;
        this.chatMetrics = chatMetrics;
        this.messageRepository = messageRepository;
        this.participantGuard = participantGuard;
        this.messagingTemplate = messagingTemplate;
        this.presenceService = presenceService;
        this.messageEventPublisher = messageEventPublisher;
        this.messageTtl = messageTtl;
    }

    public MessageResponse send(UUID me, UUID matchId, String text) {
        MatchSummary match = participantGuard.requireParticipant(me, matchId);
        Instant now = Instant.now();
        Message saved = messageRepository.save(
                new Message(matchId.toString(), me.toString(), text.strip(), now, now.plus(messageTtl)));
        MessageResponse response = MessageResponse.from(saved);
        chatMetrics.messageSent();
        activity.count(me, "msgs", 1);   // a count for the admin dashboard; never the text
        messagingTemplate.convertAndSend("/topic/chats/" + matchId, response);

        UUID recipient = match.otherThan(me);
        boolean recipientOnline = presenceService.presenceOf(recipient.toString()).online();
        messageEventPublisher.publish(
                MessageSentEvent.of(saved.getId(), matchId, me, recipient, recipientOnline, now));
        // scam screening runs AFTER delivery, in ai-service: chatting never waits for the AI
        messageEventPublisher.publish(
                MessageScreeningEvent.of(saved.getId(), matchId, me, recipient, saved.getText(), now));
        return response;
    }

    /** A sticker (free) or a gift (3 ❤, paid from the sender's wallet first). The text field holds the sticker id. */
    public MessageResponse sendSticker(UUID me, UUID matchId, String stickerId) {
        boolean gift = com.softlaunch.chat.sticker.Stickers.GIFTS.contains(stickerId);
        if (!gift && !com.softlaunch.chat.sticker.Stickers.FREE.contains(stickerId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unknown sticker");
        }
        MatchSummary match = participantGuard.requireParticipant(me, matchId);
        if (gift) {
            try {
                hearts.spend(me, java.util.Map.of("item", "GIFT", "ref", matchId.toString()));
            } catch (feign.FeignException.FeignClientException e) {
                if (e.status() == 402) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.PAYMENT_REQUIRED, "A gift costs 3 ❤.");
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "That gift couldn't be sent.");
            } catch (feign.FeignException e) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Gifts are unavailable right now.");
            }
        }
        Instant now = Instant.now();
        Message saved = messageRepository.save(new Message(matchId.toString(), me.toString(), stickerId, now, now.plus(messageTtl),
                gift ? com.softlaunch.chat.model.MessageType.GIFT : com.softlaunch.chat.model.MessageType.STICKER));
        MessageResponse response = MessageResponse.from(saved);
        chatMetrics.messageSent();
        activity.count(me, "msgs", 1);
        messagingTemplate.convertAndSend("/topic/chats/" + matchId, response);
        UUID recipient = match.otherThan(me);
        messageEventPublisher.publish(MessageSentEvent.of(saved.getId(), matchId, me, recipient,
                presenceService.presenceOf(recipient.toString()).online(), now));
        return response;
    }

    /** "Keep chat" (5 ❤, paid in user-service): every message in this chat lives 24 hours longer. */
    public int keep(UUID requester, UUID matchId, int hours) {
        participantGuard.requireParticipant(requester, matchId);   // only someone IN the chat can extend it
        List<Message> all = messageRepository.findByMatchId(matchId.toString());
        all.forEach(m -> m.extendBy(java.time.Duration.ofHours(hours)));
        messageRepository.saveAll(all);
        return all.size();
    }

    public List<MessageResponse> history(UUID me, UUID matchId) {
        participantGuard.requireParticipant(me, matchId);
        List<MessageResponse> latestFirst = messageRepository.findTop50ByMatchIdOrderBySentAtDesc(matchId.toString())
                .stream()
                .map(MessageResponse::from)
                .toList();
        List<MessageResponse> oldestFirst = new ArrayList<>(latestFirst);
        Collections.reverse(oldestFirst);
        return oldestFirst;
    }
}