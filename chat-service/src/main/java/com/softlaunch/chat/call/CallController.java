package com.softlaunch.chat.call;

import com.softlaunch.chat.call.CallDtos.*;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * REST: consent + ICE servers (X-User-Id from the gateway).
 * STOMP (/app/calls/{matchId}/...): ring, accept, decline, end, signal. The user comes from the authenticated socket.
 */
@Controller
public class CallController {

    private final CallService calls;

    public CallController(CallService calls) {
        this.calls = calls;
    }

    @GetMapping("/api/chats/{matchId}/calls/consent")
    @ResponseBody
    public Consent consent(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID matchId) {
        return calls.consent(me, matchId);
    }

    @PutMapping("/api/chats/{matchId}/calls/consent")
    @ResponseBody
    public Consent setConsent(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID matchId, @RequestBody ConsentRequest req) {
        return calls.setConsent(me, matchId, req.mode());
    }

    @GetMapping("/api/chats/{matchId}/calls/status")
    @ResponseBody
    public CallRules.Status status(@RequestHeader("X-User-Id") UUID me, @PathVariable UUID matchId) {
        return calls.status(me, matchId);
    }

    @GetMapping("/api/chats/calls/ice-servers")
    @ResponseBody
    public IceServers iceServers(@RequestHeader("X-User-Id") UUID me) {
        return calls.iceServers(me);
    }

    @MessageMapping("/calls/{matchId}/ring")
    public void ring(@DestinationVariable UUID matchId, RingRequest req, Principal p) {
        calls.ring(id(p), matchId, req != null && req.video());
    }

    @MessageMapping("/calls/{matchId}/accept")
    public void accept(@DestinationVariable UUID matchId, @Header(name = "call-id", required = false) String callId, Principal p) {
        calls.accept(id(p), matchId, callId);
    }

    @MessageMapping("/calls/{matchId}/decline")
    public void decline(@DestinationVariable UUID matchId, @Header(name = "call-id", required = false) String callId, Principal p) {
        calls.decline(id(p), matchId, callId);
    }

    @MessageMapping("/calls/{matchId}/end")
    public void end(@DestinationVariable UUID matchId, @Header(name = "call-id", required = false) String callId, Principal p) {
        calls.end(id(p), matchId, callId);
    }

    @MessageMapping("/calls/{matchId}/signal")
    public void signal(@DestinationVariable UUID matchId, @Header(name = "call-id", required = false) String callId,
                       SignalRequest req, Principal p) {
        calls.signal(id(p), matchId, callId, req);
    }

    private static UUID id(Principal p) {
        return UUID.fromString(p.getName());
    }
}
