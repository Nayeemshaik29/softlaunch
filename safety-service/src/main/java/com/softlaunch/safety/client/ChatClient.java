package com.softlaunch.safety.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "chat-service")
public interface ChatClient {

    record ChatMessage(String id, String senderId, String text, Instant sentAt) { }

    @GetMapping("/internal/chats/{matchId}/messages")
    List<ChatMessage> recentMessages(@PathVariable("matchId") UUID matchId, @RequestParam("limit") int limit);
}
