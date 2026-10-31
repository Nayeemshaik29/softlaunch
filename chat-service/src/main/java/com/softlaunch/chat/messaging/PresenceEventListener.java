package com.softlaunch.chat.messaging;

import com.softlaunch.chat.dto.PresenceResponse;
import com.softlaunch.chat.security.StompAuthInterceptor;
import com.softlaunch.chat.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.Set;

@Component
public class PresenceEventListener {

    private static final String CHAT_TOPIC = "/topic/chats/[0-9a-fA-F-]{36}";

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceEventListener(PresenceService presenceService, SimpMessagingTemplate messagingTemplate) {
        this.presenceService = presenceService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        if (event.getUser() == null) {
            return;
        }
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        presenceService.connected(event.getUser().getName(), sessionId);
    }

    @EventListener
    public void onSubscribed(SessionSubscribeEvent event) {
        String destination = StompHeaderAccessor.wrap(event.getMessage()).getDestination();
        if (event.getUser() == null || destination == null || !destination.matches(CHAT_TOPIC)) {
            return;
        }
        messagingTemplate.convertAndSend(destination + "/presence",
                presenceService.presenceOf(event.getUser().getName()));
    }

    @EventListener
    @SuppressWarnings("unchecked")
    public void onDisconnected(SessionDisconnectEvent event) {
        if (event.getUser() == null) {
            return;
        }
        String userId = event.getUser().getName();
        if (!presenceService.disconnected(userId, event.getSessionId())) {
            return;
        }
        Map<String, Object> attributes = StompHeaderAccessor.wrap(event.getMessage()).getSessionAttributes();
        Set<String> chats = attributes == null
                ? Set.of()
                : (Set<String>) attributes.getOrDefault(StompAuthInterceptor.SESSION_CHATS, Set.of());
        PresenceResponse offline = presenceService.presenceOf(userId);
        chats.forEach(matchId -> messagingTemplate.convertAndSend("/topic/chats/" + matchId + "/presence", offline));
    }
}