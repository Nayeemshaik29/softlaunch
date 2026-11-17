package com.softlaunch.ai.openers;

import com.softlaunch.ai.client.MatchClient;
import com.softlaunch.ai.client.MatchClient.MatchSummary;
import com.softlaunch.ai.client.UserProfileClient;
import com.softlaunch.ai.client.UserProfileClient.ProfileView;
import com.softlaunch.ai.exception.NotFoundException;
import com.softlaunch.ai.observability.AiMetrics;
import feign.FeignException;
import io.micrometer.context.ContextSnapshotFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Stuck? Try one of these": 3 first messages for a match, written by the local LLM.
 * The AI is optional: if it is slow, down, or says something unsafe, the user gets template openers.
 * Never logs profile text, prompts or openers (they are personal data).
 */
@Service
public class OpenerService {

    private static final Logger log = LoggerFactory.getLogger(OpenerService.class);

    private final MatchClient matchClient;
    private final UserProfileClient userProfileClient;
    private final ChatClient chatClient;
    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final AiProperties props;
    private final AiMetrics metrics;
    // the LLM call runs on a virtual thread so we can stop waiting after `timeout`
    private final ExecutorService llmExecutor = Executors.newVirtualThreadPerTaskExecutor();
    // copies the tracing context to that thread, so the LLM call shows up in the same Zipkin trace
    private final ContextSnapshotFactory snapshots = ContextSnapshotFactory.builder().build();

    public OpenerService(MatchClient matchClient, UserProfileClient userProfileClient, ChatClient.Builder chatClientBuilder,
                         StringRedisTemplate redis, JsonMapper json, AiProperties props, AiMetrics metrics) {
        this.matchClient = matchClient;
        this.userProfileClient = userProfileClient;
        this.chatClient = chatClientBuilder.defaultSystem(OpenerPrompt.SYSTEM).build();
        this.redis = redis;
        this.json = json;
        this.props = props;
        this.metrics = metrics;
    }

    public OpenersResponse openers(UUID me, UUID matchId, boolean refresh) {
        MatchSummary match = requireMyMatch(me, matchId);
        String cacheKey = "ai:openers:" + matchId + ":" + me;
        if (!refresh) {
            OpenersResponse cached = readCache(cacheKey);
            if (cached != null) {
                metrics.servedFromCache();
                return cached;
            }
        }

        Map<UUID, ProfileView> profiles = userProfileClient.getProfiles(List.of(me, match.otherThan(me))).stream()
                .collect(Collectors.toMap(ProfileView::userId, Function.identity()));
        ProfileView mine = profiles.get(me);
        ProfileView theirs = profiles.get(match.otherThan(me));
        if (mine == null || theirs == null) {
            throw new NotFoundException("Profile not found");   // a profile is required before matching anyway
        }

        List<String> aiOpeners;
        try {
            aiOpeners = askLlm(mine, theirs);
        } catch (TimeoutException e) {
            log.warn("LLM took longer than {}, serving template openers for match {}", props.timeout(), matchId);
            metrics.servedFallback(AiMetrics.TIMEOUT);
            return new OpenersResponse(FallbackOpeners.forProfiles(mine, theirs), "fallback");
        } catch (Exception e) {
            log.warn("LLM call failed ({}), serving template openers for match {}", e.getClass().getSimpleName(), matchId);
            metrics.servedFallback(AiMetrics.ERROR);
            return new OpenersResponse(FallbackOpeners.forProfiles(mine, theirs), "fallback");
        }

        List<String> acceptable = aiOpeners.stream().filter(OpenerSafety::isAcceptable).map(String::strip).toList();
        metrics.rejected(aiOpeners.size() - acceptable.size());
        List<String> safe = acceptable.stream().distinct().limit(3).toList();
        if (safe.isEmpty()) {
            log.warn("All AI openers failed the safety check, serving templates for match {}", matchId);
            metrics.servedFallback(AiMetrics.UNSAFE_OUTPUT);
            return new OpenersResponse(FallbackOpeners.forProfiles(mine, theirs), "fallback");
        }

        List<String> result = new ArrayList<>(safe);
        FallbackOpeners.forProfiles(mine, theirs).stream().filter(t -> result.size() < 3).forEach(result::add);
        OpenersResponse response = new OpenersResponse(List.copyOf(result), "ai");
        writeCache(cacheKey, response);   // only AI answers are cached: after a fallback the next try asks the AI again
        metrics.servedFromAi();
        return response;
    }

    private List<String> askLlm(ProfileView mine, ProfileView theirs) throws Exception {
        String userMessage = OpenerPrompt.userMessage(mine, theirs, json);
        Future<LlmOpeners> future = llmExecutor.submit(snapshots.captureAll().wrap(() -> chatClient.prompt()
                .user(userMessage)
                .options(OllamaChatOptions.builder().format("json"))   // Ollama "JSON mode": output must be valid JSON
                .call()
                .entity(LlmOpeners.class)));
        try {
            LlmOpeners answer = future.get(props.timeout().toMillis(), TimeUnit.MILLISECONDS);
            return answer == null || answer.openers() == null ? List.of() : answer.openers();
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    /** Cached openers quote the other person's profile: remove them when a match or account goes away. */
    public long evictMatch(UUID matchId) {
        return evict("ai:openers:" + matchId + ":*");
    }

    public long evictUser(UUID userId) {
        return evict("ai:openers:*:" + userId);
    }

    private long evict(String pattern) {
        try {
            List<String> keys = new ArrayList<>();
            try (var cursor = redis.scan(org.springframework.data.redis.core.ScanOptions.scanOptions().match(pattern).count(500).build())) {
                cursor.forEachRemaining(keys::add);
            }
            return keys.isEmpty() ? 0 : redis.delete(keys);
        } catch (DataAccessException e) {
            log.warn("Could not evict cached openers: {}", e.getMessage());
            return 0;   // they expire within cache-ttl anyway
        }
    }

    private MatchSummary requireMyMatch(UUID me, UUID matchId) {
        try {
            MatchSummary match = matchClient.getMatch(matchId);
            if (match.includes(me)) {
                return match;
            }
        } catch (FeignException.NotFound e) {
            // fall through: same answer for "no such match" and "not your match", so ids can't be probed
        }
        throw new NotFoundException("Match not found");
    }

    private OpenersResponse readCache(String key) {
        try {
            String cached = redis.opsForValue().get(key);
            return cached == null ? null : json.readValue(cached, OpenersResponse.class);
        } catch (DataAccessException e) {
            return null;   // Redis down: just ask the AI
        }
    }

    private void writeCache(String key, OpenersResponse response) {
        try {
            redis.opsForValue().set(key, json.writeValueAsString(response), props.cacheTtl());
        } catch (DataAccessException e) {
            log.debug("Could not cache openers: {}", e.getMessage());
        }
    }

    /** The JSON shape we ask the LLM for. Anything else fails to parse and falls back to templates. */
    record LlmOpeners(List<String> openers) {
    }
}
