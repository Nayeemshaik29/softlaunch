package com.softlaunch.chat.controller;

import com.softlaunch.chat.dto.ScreenshotRequest;
import com.softlaunch.chat.service.ScreenshotService;

import com.softlaunch.chat.client.MatchSummary;
import com.softlaunch.chat.dto.SendMessageRequest;
import com.softlaunch.chat.dto.TypingEvent;
import com.softlaunch.chat.dto.TypingRequest;
import com.softlaunch.chat.security.StompAuthInterceptor;
import com.softlaunch.chat.service.ChatService;
import com.softlaunch.chat.service.ParticipantGuard;
import com.softlaunch.chat.service.PresenceService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final PresenceService presenceService;
    private final ParticipantGuard participantGuard;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScreenshotService screenshotService;

    public ChatWebSocketController(ChatService chatService, PresenceService presenceService,
                                   ParticipantGuard participantGuard, SimpMessagingTemplate messagingTemplate,
                                   ScreenshotService screenshotService) {
        this.screenshotService = screenshotService;
        this.chatService = chatService;
        this.presenceService = presenceService;
        this.participantGuard = participantGuard;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chats/{matchId}/send")
    public void send(@DestinationVariable UUID matchId, SendMessageRequest request, Principal principal) {
        if (request.text() == null || request.text().isBlank() || request.text().length() > 1000) {
            return;
        }
        chatService.send(UUID.fromString(principal.getName()), matchId, request.text());
    }

    @MessageMapping("/chats/{matchId}/typing")
    public void typing(@DestinationVariable UUID matchId, TypingRequest request, Principal principal,
                       SimpMessageHeaderAccessor accessor) {
        if (!isAllowed(accessor, matchId)) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/chats/" + matchId + "/typing",
                new TypingEvent(principal.getName(), request.typing()));
    }

    @MessageMapping("/chats/{matchId}/presence")
    public void presence(@DestinationVariable UUID matchId, Principal principal, SimpMessageHeaderAccessor accessor) {
        if (!isAllowed(accessor, matchId)) {
            return;
        }
        UUID me = UUID.fromString(principal.getName());
        MatchSummary match = participantGuard.requireParticipant(me, matchId);
        messagingTemplate.convertAndSend("/topic/chats/" + matchId + "/presence",
                presenceService.presenceOf(match.otherThan(me).toString()));
    }

    @MessageMapping("/chats/{matchId}/screenshot")
    public void screenshot(@DestinationVariable UUID matchId, ScreenshotRequest request, Principal principal,
                           SimpMessageHeaderAccessor accessor) {
        if (!isAllowed(accessor, matchId) || request == null || request.kind() == null) {
            return;
        }
        screenshotService.captured(UUID.fromString(principal.getName()), matchId, request.kind());
    }

    @MessageMapping("/presence/ping")
    public void ping(Principal principal) {
        presenceService.heartbeat(principal.getName());
    }

    @SuppressWarnings("unchecked")
    private boolean isAllowed(SimpMessageHeaderAccessor accessor, UUID matchId) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes == null) {
            return false;
        }
        Set<String> chats = (Set<String>) attributes.get(StompAuthInterceptor.SESSION_CHATS);
        return chats != null && chats.contains(matchId.toString());
    }
}