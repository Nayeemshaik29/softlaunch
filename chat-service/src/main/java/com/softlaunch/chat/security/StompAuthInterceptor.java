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
import java.util.UUID;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final String CHAT_TOPIC_PREFIX = "/topic/chats/";

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
            authorizeSubscription(accessor.getUser(), accessor.getDestination());
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

    private void authorizeSubscription(Principal user, String destination) {
        if (user == null || destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
            throw new MessagingException("Forbidden");
        }
        try {
            UUID matchId = UUID.fromString(destination.substring(CHAT_TOPIC_PREFIX.length()));
            participantGuard.requireParticipant(UUID.fromString(user.getName()), matchId);
        } catch (RuntimeException e) {
            throw new MessagingException("Forbidden");
        }
    }
}