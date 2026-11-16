package com.softlaunch.ai.openers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class OpenerController {

    private final OpenerService openerService;

    public OpenerController(OpenerService openerService) {
        this.openerService = openerService;
    }

    /** refresh=true skips the cache ("show me different ones"); the gateway rate-limits this route harder. */
    @GetMapping("/matches/{matchId}/openers")
    public OpenersResponse openers(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID matchId,
                                   @RequestParam(defaultValue = "false") boolean refresh) {
        return openerService.openers(me, matchId, refresh);
    }
}
