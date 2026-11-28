package com.softlaunch.user.hearts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class HeartController {

    private final HeartService hearts;

    public HeartController(HeartService hearts) {
        this.hearts = hearts;
    }

    public record SpendRequest(@NotNull HeartItem item, @Size(max = 80) String ref) {
    }

    @GetMapping("/api/users/me/hearts")
    public HeartService.Wallet wallet(@RequestHeader("X-User-Id") UUID me) {
        return hearts.wallet(me);
    }

    /** From the app: crown, call pass, keep chat. (Notes and gifts are spent by match/chat-service.) */
    @PostMapping("/api/users/me/hearts/spend")
    public HeartService.Spent spend(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody SpendRequest r) {
        if (r.item() == HeartItem.NOTE || r.item() == HeartItem.GIFT) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Send it from the like or the chat.");
        }
        return hearts.spend(me, r.item(), r.ref());
    }

    /** Internal (not routed by the gateway): match-service (notes) and chat-service (gifts). */
    @PostMapping("/internal/users/{userId}/hearts/spend")
    public HeartService.Spent spendInternal(@PathVariable UUID userId, @Valid @RequestBody SpendRequest r) {
        return hearts.spend(userId, r.item(), r.ref());
    }
}
