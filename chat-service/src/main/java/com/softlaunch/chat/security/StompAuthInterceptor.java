package com.softlaunch.chat.security;

import com.softlaunch.chat.service.ParticipantGuard;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    public static final String SESSION_CHATS = "chats";

    private static final String CHAT_TOPIC_PREFIX = "/topic/chats/";
    private static final Set<String> ALLOWED_SUFFIXES = Set.of("typing", "presence", "alerts");

    private final JwtVerifier jwtVerifier;
    private final ParticipantGuard participantGuard;

    public StompAuthInterceptor(JwtVerifier jwtVerifier, ParticipantGuard participantGuard) {
        this.jwtVerifier = jwtVerifier;
        this.participantGuard = participantGuard;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private Principal authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingException("Unauthorized");
        }
        try {
            return new StompPrincipal(jwtVerifier.verify(authHeader.substring(7)).toString());
        } catch (RuntimeException e) {
            throw new MessagingException("Unauthorized");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        if (user == null || destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
            throw new MessagingException("Forbidden");
        }
        try {
            String[] parts = destination.substring(CHAT_TOPIC_PREFIX.length()).split("/", 2);
            if (parts.length == 2 && !ALLOWED_SUFFIXES.contains(parts[1])) {
                throw new MessagingException("Forbidden");
            }
            UUID matchId = UUID.fromString(parts[0]);
            participantGuard.requireParticipant(UUID.fromString(user.getName()), matchId);
            rememberChat(accessor, matchId);
        } catch (RuntimeException e) {
            throw new MessagingException("Forbidden");
        }
    }

    @SuppressWarnings("unchecked")
    private void rememberChat(StompHeaderAccessor accessor, UUID matchId) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null) {
            ((Set<String>) attributes.computeIfAbsent(SESSION_CHATS, k -> ConcurrentHashMap.newKeySet()))
                    .add(matchId.toString());
        }
    }
}