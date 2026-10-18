package com.softlaunch.chat.controller;

import com.softlaunch.chat.dto.MessageResponse;
import com.softlaunch.chat.dto.SendMessageRequest;
import com.softlaunch.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{matchId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@RequestHeader("X-User-Id") UUID me,
                                @PathVariable UUID matchId,
                                @Valid @RequestBody SendMessageRequest request) {
        return chatService.send(me, matchId, request.text());
    }

    @GetMapping("/{matchId}/messages")
    public List<MessageResponse> history(@RequestHeader("X-User-Id") UUID me,
                                         @PathVariable UUID matchId) {
        return chatService.history(me, matchId);
    }
}