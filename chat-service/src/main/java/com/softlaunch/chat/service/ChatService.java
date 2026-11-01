package com.softlaunch.chat.service;

import com.softlaunch.chat.client.MatchSummary;
import com.softlaunch.chat.dto.MessageResponse;
import com.softlaunch.chat.event.MessageEventPublisher;
import com.softlaunch.chat.event.MessageSentEvent;
import com.softlaunch.chat.model.Message;
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

    public ChatService(MessageRepository messageRepository, ParticipantGuard participantGuard,
                       SimpMessagingTemplate messagingTemplate, PresenceService presenceService,
                       MessageEventPublisher messageEventPublisher,
                       @Value("${softlaunch.chat.message-ttl}") Duration messageTtl) {
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
        messagingTemplate.convertAndSend("/topic/chats/" + matchId, response);

        UUID recipient = match.otherThan(me);
        boolean recipientOnline = presenceService.presenceOf(recipient.toString()).online();
        messageEventPublisher.publish(
                MessageSentEvent.of(saved.getId(), matchId, me, recipient, recipientOnline, now));
        return response;
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