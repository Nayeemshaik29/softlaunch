package com.softlaunch.chat.controller;

import com.softlaunch.chat.dto.MessageResponse;
import com.softlaunch.chat.repository.MessageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Service-to-service only (the gateway routes /api/chats/**, never /internal/**).
 * Used by safety-service to copy evidence into a report before the 24h TTL deletes it.
 */
@RestController
@RequestMapping("/internal/chats")
public class InternalChatController {

    private static final int MAX_LIMIT = 50;

    private final MessageRepository messageRepository;

    public InternalChatController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/{matchId}/messages")
    public List<MessageResponse> recentMessages(@PathVariable UUID matchId,
                                                @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        return messageRepository.findTop50ByMatchIdOrderBySentAtDesc(matchId.toString()).stream()
                .limit(safeLimit)
                .sorted(Comparator.comparing(m -> m.getSentAt()))      // oldest first: reads like a conversation
                .map(MessageResponse::from)
                .toList();
    }
}
