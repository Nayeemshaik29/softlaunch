package com.softlaunch.match.messaging;

import com.softlaunch.match.event.BlockChangedEvent;
import com.softlaunch.match.exception.InvalidEventException;
import com.softlaunch.match.service.BlockSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class BlockEventListener {

    private static final Logger log = LoggerFactory.getLogger(BlockEventListener.class);

    private final BlockSyncService blockSyncService;
    private final JsonMapper jsonMapper;

    public BlockEventListener(BlockSyncService blockSyncService, JsonMapper jsonMapper) {
        this.blockSyncService = blockSyncService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "block.changed")
    public void onBlockChanged(String payload) {
        log.info("Received block.changed: {}", payload);
        BlockChangedEvent event = jsonMapper.readValue(payload, BlockChangedEvent.class);
        if (event.blockerId() == null || event.blockedId() == null) {
            throw new InvalidEventException("Invalid BlockChangedEvent: missing blockerId or blockedId");
        }
        blockSyncService.apply(event);
    }
}
