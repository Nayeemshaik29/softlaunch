package com.softlaunch.chat.call;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CallDtos {
    private CallDtos() {
    }

    /** mine = what I agreed to; theirs = what they agreed to; allowed = what a call may use right now. */
    public record Consent(CallMode mine, CallMode theirs, CallMode allowed) {
    }

    public record ConsentRequest(CallMode mode) {
    }

    public record IceServer(List<String> urls, String username, String credential) {
    }

    public record IceServers(List<IceServer> iceServers, long ttlSeconds) {
    }

    public record RingRequest(boolean video) {
    }

    /** kind: offer | answer | ice | video-request | video-accept | video-decline. payload: SDP/ICE as sent by the browser. */
    public record SignalRequest(String kind, Map<String, Object> payload) {
    }

    /**
     * Everything that reaches a phone/browser on /user/queue/calls.
     * type: ring | ringing | accepted | declined | ended | busy | signal | consent | error
     */
    public record CallEvent(String type, UUID matchId, String callId, String from, Boolean video, String kind,
                            Map<String, Object> payload, Consent consent, String reason) {
        static CallEvent of(String type, UUID matchId, String callId, String from, Boolean video) {
            return new CallEvent(type, matchId, callId, from, video, null, null, null, null);
        }
    }
}
