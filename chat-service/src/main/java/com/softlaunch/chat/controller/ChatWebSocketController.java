package com.softlaunch.chat.controller;

import com.softlaunch.chat.dto.SendMessageRequest;
import com.softlaunch.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    public ChatWebSocketController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chats/{matchId}/send")
    public void send(@DestinationVariable UUID matchId, SendMessageRequest request, Principal principal) {
        if (request.text() == null || request.text().isBlank() || request.text().length() > 1000) {
            return;
        }
        chatService.send(UUID.fromString(principal.getName()), matchId, request.text());
    }
}