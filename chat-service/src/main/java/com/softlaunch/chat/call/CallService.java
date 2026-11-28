package com.softlaunch.chat.call;

import com.softlaunch.chat.activity.ActivityRecorder;
import com.softlaunch.chat.call.CallDtos.*;
import com.softlaunch.chat.client.MatchSummary;
import com.softlaunch.chat.service.ParticipantGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Audio/video calls between two matched people. We never touch the media: browsers talk to each other (WebRTC,
 * encrypted end to end by DTLS-SRTP), and this class only (1) stores consent, (2) rings, and (3) passes the
 * connection details ("signals") between exactly those two people. Nothing is recorded.
 *
 * Rules: a call needs BOTH people's consent for that match; video needs both to have said "video".
 * Redis keys: call:consent:{matchId} (hash user→mode) · call:active:{matchId} (hash) · call:busy:{userId} → matchId
 */
@Service
public class CallService {

    public static final String QUEUE = "/queue/calls";
    private static final Logger log = LoggerFactory.getLogger(CallService.class);
    private static final Set<String> SIGNAL_KINDS = Set.of("offer", "answer", "ice", "video-request", "video-accept", "video-decline");
    private static final int MAX_SIGNAL_CHARS = 20_000;

    private final StringRedisTemplate redis;
    private final ParticipantGuard guard;
    private final SimpMessagingTemplate stomp;
    private final CallProperties props;
    private final ActivityRecorder activity;
    private final CallRules rules;

    public CallService(StringRedisTemplate redis, ParticipantGuard guard, SimpMessagingTemplate stomp,
                       CallProperties props, ActivityRecorder activity, CallRules rules) {
        this.rules = rules;
        this.redis = redis;
        this.guard = guard;
        this.stomp = stomp;
        this.props = props;
        this.activity = activity;
    }

    // ------------------------------------------------------------------ consent

    public Consent consent(UUID me, UUID matchId) {
        MatchSummary match = guard.requireParticipant(me, matchId);
        return consentOf(matchId, me, match.otherThan(me));
    }

    public Consent setConsent(UUID me, UUID matchId, CallMode mode) {
        MatchSummary match = guard.requireParticipant(me, matchId);
        String key = consentKey(matchId);
        if (mode == null || mode == CallMode.OFF) {
            redis.opsForHash().delete(key, me.toString());
        } else {
            redis.opsForHash().put(key, me.toString(), mode.name());
        }
        UUID other = match.otherThan(me);
        Consent mine = consentOf(matchId, me, other);
        // the other person's screen updates live ("Sameer is open to calls")
        send(other, new CallEvent("consent", matchId, null, me.toString(), null, null, null, consentOf(matchId, other, me), null));
        // withdrawing consent ends a running call
        Map<Object, Object> active = redis.opsForHash().entries(activeKey(matchId));
        if (!active.isEmpty() && (mine.allowed() == CallMode.OFF
                || (Boolean.parseBoolean((String) active.get("video")) && mine.allowed() != CallMode.VIDEO))) {
            finish(matchId, active, me, "consent_withdrawn");
        }
        return mine;
    }

    private Consent consentOf(UUID matchId, UUID me, UUID other) {
        Map<Object, Object> all = redis.opsForHash().entries(consentKey(matchId));
        CallMode mine = parse(all.get(me.toString()));
        CallMode theirs = parse(all.get(other.toString()));
        return new Consent(mine, theirs, CallMode.lowerOf(mine, theirs));
    }

    /** Timer, day tracker and paywall in the chat. */
    public CallRules.Status status(UUID me, UUID matchId) {
        MatchSummary match = guard.requireParticipant(me, matchId);
        return rules.status(me, match.otherThan(me), matchId, consentOf(matchId, me, match.otherThan(me)));
    }

    // ------------------------------------------------------------------ ICE servers

    public IceServers iceServers(UUID me) {
        List<IceServer> servers = new ArrayList<>();
        if (!props.stunUrls().isEmpty()) {
            servers.add(new IceServer(props.stunUrls(), null, null));
        }
        if (!props.turnUrls().isEmpty() && props.turnSecret() != null && !props.turnSecret().isBlank()) {
            long expiry = Instant.now().plus(props.credentialTtl()).getEpochSecond();
            String username = expiry + ":" + me;
            servers.add(new IceServer(props.turnUrls(), username, hmac(props.turnSecret(), username)));
        }
        return new IceServers(servers, props.credentialTtl().toSeconds());
    }

    static String hmac(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("TURN credential", e);
        }
    }

    // ------------------------------------------------------------------ call lifecycle

    public void ring(UUID me, UUID matchId, boolean video) {
        MatchSummary match = guard.requireParticipant(me, matchId);
        UUID other = match.otherThan(me);
        Consent c = consentOf(matchId, me, other);
        if (c.allowed() == CallMode.OFF || (video && c.allowed() != CallMode.VIDEO)) {
            send(me, error(matchId, "not_allowed"));
            return;
        }
        String blocked = rules.problem(me, other, matchId, video);
        if (blocked != null) {
            send(me, error(matchId, blocked));
            return;
        }
        if (Boolean.FALSE.equals(redis.opsForValue().setIfAbsent("call:ringlimit:" + me, "1", Duration.ofSeconds(5)))) {
            send(me, error(matchId, "slow_down"));
            return;
        }
        String callId = UUID.randomUUID().toString();
        // both must be free; the busy keys are claimed atomically so two calls can't cross
        if (!claim(me, matchId)) {
            send(me, error(matchId, "you_are_busy"));
            return;
        }
        if (!claim(other, matchId)) {
            redis.delete(busyKey(me));
            send(me, CallEvent.of("busy", matchId, callId, other.toString(), video));
            return;
        }
        Map<String, String> call = Map.of("callId", callId, "caller", me.toString(), "callee", other.toString(),
                "video", Boolean.toString(video), "state", "ringing");
        redis.opsForHash().putAll(activeKey(matchId), call);
        redis.expire(activeKey(matchId), props.ringTimeout());
        send(me, CallEvent.of("ringing", matchId, callId, me.toString(), video));
        send(other, CallEvent.of("ring", matchId, callId, me.toString(), video));
        activity.feed(me, video ? "call_video" : "call_audio");
    }

    public void accept(UUID me, UUID matchId, String callId) {
        Map<Object, Object> call = current(matchId, callId);
        if (call == null || !me.toString().equals(call.get("callee")) || !"ringing".equals(call.get("state"))) {
            send(me, error(matchId, "gone"));
            return;
        }
        redis.opsForHash().put(activeKey(matchId), "state", "active");
        rules.callAnswered(matchId, Boolean.parseBoolean((String) call.get("video")));
        redis.opsForHash().put(activeKey(matchId), "startedAt", Instant.now().toString());
        redis.expire(activeKey(matchId), props.maxCall());
        redis.expire(busyKey(me), props.maxCall());
        redis.expire(busyKey(UUID.fromString((String) call.get("caller"))), props.maxCall());
        send(UUID.fromString((String) call.get("caller")), CallEvent.of("accepted", matchId, callId, me.toString(), null));
    }

    public void decline(UUID me, UUID matchId, String callId) {
        Map<Object, Object> call = current(matchId, callId);
        if (call != null && participant(call, me)) {
            finish(matchId, call, me, "declined");
        }
    }

    public void end(UUID me, UUID matchId, String callId) {
        Map<Object, Object> call = current(matchId, callId);
        if (call != null && participant(call, me)) {
            finish(matchId, call, me, "ended");
        }
    }

    /** Pass offer/answer/ICE (and the video-upgrade question) to the other person, only within a live call. */
    public void signal(UUID me, UUID matchId, String callId, SignalRequest req) {
        if (req == null || req.kind() == null || !SIGNAL_KINDS.contains(req.kind())) return;
        if (req.payload() != null && req.payload().toString().length() > MAX_SIGNAL_CHARS) return;
        Map<Object, Object> call = current(matchId, callId);
        if (call == null || !participant(call, me)) return;
        UUID other = otherIn(call, me);
        if (req.kind().equals("video-request") || req.kind().equals("video-accept")) {
            // turning video on mid-call still needs both people's "video" consent
            if (consentOf(matchId, me, other).allowed() != CallMode.VIDEO) {
                send(me, error(matchId, "video_not_allowed"));
                return;
            }
            String blocked = rules.problem(me, other, matchId, true);
            if (blocked != null) {
                send(me, error(matchId, blocked));
                return;
            }
            if (req.kind().equals("video-accept")) {
                redis.opsForHash().put(activeKey(matchId), "video", "true");
                rules.callAnswered(matchId, true);
            }
        }
        send(other, new CallEvent("signal", matchId, callId, me.toString(), null, req.kind(), req.payload(), null, null));
    }

    /** Websocket dropped (app closed, network lost): end whatever call this person was in. */
    public void dropped(UUID me) {
        String matchId = redis.opsForValue().get(busyKey(me));
        if (matchId == null) return;
        Map<Object, Object> call = redis.opsForHash().entries(activeKey(UUID.fromString(matchId)));
        if (!call.isEmpty() && participant(call, me)) finish(UUID.fromString(matchId), call, me, "dropped");
        else redis.delete(busyKey(me));
    }

    /** Unmatched or blocked: consent and any call for that match are gone. */
    public void matchRemoved(UUID matchId) {
        Map<Object, Object> call = redis.opsForHash().entries(activeKey(matchId));
        if (!call.isEmpty()) finish(matchId, call, null, "match_removed");
        redis.delete(consentKey(matchId));
        rules.forget(matchId);
    }

    private void finish(UUID matchId, Map<Object, Object> call, UUID by, String reason) {
        redis.delete(activeKey(matchId));
        UUID caller = UUID.fromString((String) call.get("caller"));
        UUID callee = UUID.fromString((String) call.get("callee"));
        redis.delete(List.of(busyKey(caller), busyKey(callee)));
        String callId = (String) call.get("callId");
        if (call.get("startedAt") != null) {
            long secs = Math.max(0, Duration.between(Instant.parse((String) call.get("startedAt")), Instant.now()).toSeconds());
            for (UUID u : List.of(caller, callee)) {
                activity.count(u, "calls", 1);
                activity.count(u, "callSecs", secs);
            }
        }
        CallEvent ended = new CallEvent("ended".equals(reason) || "dropped".equals(reason) ? "ended" : "declined".equals(reason) ? "declined" : "ended",
                matchId, callId, by == null ? null : by.toString(), null, null, null, null, reason);
        for (UUID u : List.of(caller, callee)) {
            if (!u.equals(by)) send(u, ended);
        }
        log.info("Call {} on match {} finished: {}", callId, matchId, reason);
    }

    // ------------------------------------------------------------------ helpers

    private Map<Object, Object> current(UUID matchId, String callId) {
        Map<Object, Object> call = redis.opsForHash().entries(activeKey(matchId));
        if (call.isEmpty() || callId == null || !callId.equals(call.get("callId"))) return null;
        return call;
    }

    private boolean claim(UUID user, UUID matchId) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(busyKey(user), matchId.toString(), props.ringTimeout()));
    }

    private static boolean participant(Map<Object, Object> call, UUID me) {
        return me.toString().equals(call.get("caller")) || me.toString().equals(call.get("callee"));
    }

    private static UUID otherIn(Map<Object, Object> call, UUID me) {
        return UUID.fromString((String) (me.toString().equals(call.get("caller")) ? call.get("callee") : call.get("caller")));
    }

    private void send(UUID user, CallEvent event) {
        stomp.convertAndSendToUser(user.toString(), QUEUE, event);
    }

    private static CallEvent error(UUID matchId, String reason) {
        return new CallEvent("error", matchId, null, null, null, null, null, null, reason);
    }

    private static CallMode parse(Object v) {
        try {
            return v == null ? CallMode.OFF : CallMode.valueOf(v.toString());
        } catch (IllegalArgumentException e) {
            return CallMode.OFF;
        }
    }

    private static String consentKey(UUID matchId) {
        return "call:consent:" + matchId;
    }

    private static String activeKey(UUID matchId) {
        return "call:active:" + matchId;
    }

    private static String busyKey(UUID user) {
        return "call:busy:" + user;
    }
}
