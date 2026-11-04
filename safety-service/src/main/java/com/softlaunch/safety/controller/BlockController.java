package com.softlaunch.safety.controller;

import com.softlaunch.safety.dto.BlockRequest;
import com.softlaunch.safety.dto.BlockResponse;
import com.softlaunch.safety.service.BlockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/safety/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlockResponse block(@RequestHeader("X-User-Id") UUID me, @Valid @RequestBody BlockRequest request) {
        return blockService.block(me, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID userId) {
        blockService.unblock(me, userId);
    }

    @GetMapping
    public List<BlockResponse> myBlocks(@RequestHeader("X-User-Id") UUID me) {
        return blockService.myBlocks(me);
    }
}
