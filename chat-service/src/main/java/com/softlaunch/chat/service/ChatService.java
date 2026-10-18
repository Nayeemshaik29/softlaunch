package com.softlaunch.chat.service;


import com.softlaunch.chat.client.MatchClient;
import com.softlaunch.chat.client.MatchSummary;
import com.softlaunch.chat.dto.MessageResponse;
import com.softlaunch.chat.repository.MessageRepository;
import com.softlaunch.chat.model.Message;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final MatchClient matchClient;
    private final Duration messageTtl;

    public ChatService(MessageRepository messageRepository, MatchClient matchClient,
                       @Value("${softlaunch.chat.message-ttl}") Duration messageTtl) {
        this.messageRepository = messageRepository;
        this.matchClient = matchClient;
        this.messageTtl = messageTtl;
    }

    public MessageResponse send(UUID me, UUID matchId, String text) {
        requireParticipant(me, matchId);
        Instant now = Instant.now();
        Message saved = messageRepository.save(
                new Message(matchId.toString(), me.toString(), text.strip(), now, now.plus(messageTtl)));
        return MessageResponse.from(saved);
    }

    public List<MessageResponse> history(UUID me, UUID matchId) {
        requireParticipant(me, matchId);
        List<MessageResponse> latestFirst = messageRepository.findTop50ByMatchIdOrderBySentAtDesc(matchId.toString())
                .stream()
                .map(MessageResponse::from)
                .toList();
        List<MessageResponse> oldestFirst = new ArrayList<>(latestFirst);
        Collections.reverse(oldestFirst);
        return oldestFirst;
    }

    private void requireParticipant(UUID me, UUID matchId) {
        MatchSummary match;
        try {
            match = matchClient.getMatch(matchId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chat temporarily unavailable");
        }
        if (!match.involves(me)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found");
        }
    }
}