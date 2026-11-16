package com.softlaunch.chat.observability;

import com.softlaunch.chat.model.CaptureKind;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business metrics for chat: messages sent, screenshot alerts, and open chat connections.
 *
 * "Connections" is a gauge counted by THIS instance (its own WebSocket sessions), not a Redis scan:
 * scanning every presence key on each 15s scrape gets slow with many users, and with 3 chat instances
 * each would report the same global number. Per-instance counts can simply be summed in Grafana.
 * One user with phone + laptop = 2 connections.
 */
@Component
public class ChatMetrics {

    private final Counter messagesSent;
    private final Map<CaptureKind, Counter> capturesAlerted = new EnumMap<>(CaptureKind.class);
    private final Map<CaptureKind, Counter> capturesDebounced = new EnumMap<>(CaptureKind.class);
    // a Set (not a counter) so a duplicate disconnect event can't push the number below the truth
    private final Set<String> openSessions = ConcurrentHashMap.newKeySet();

    public ChatMetrics(MeterRegistry registry) {
        messagesSent = Counter.builder("softlaunch.messages.sent").description("Chat messages sent").register(registry);
        for (CaptureKind kind : CaptureKind.values()) {
            capturesAlerted.put(kind, capture(registry, kind, "alerted"));
            capturesDebounced.put(kind, capture(registry, kind, "debounced"));
        }
        Gauge.builder("softlaunch.chat.connections", openSessions, Set::size)
                .description("Open, authenticated chat WebSocket connections on this instance")
                .register(registry);
    }

    private static Counter capture(MeterRegistry registry, CaptureKind kind, String result) {
        return Counter.builder("softlaunch.screen.captures")
                .description("Screenshot / screen-recording reports from the app")
                .tag("kind", kind.name())
                .tag("result", result)
                .register(registry);
    }

    public void messageSent() {
        messagesSent.increment();
    }

    public void captureAlerted(CaptureKind kind) {
        capturesAlerted.get(kind).increment();
    }

    public void captureDebounced(CaptureKind kind) {
        capturesDebounced.get(kind).increment();
    }

    @EventListener
    void onConnected(SessionConnectedEvent event) {
        if (event.getUser() != null) {
            openSessions.add(StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
        }
    }

    @EventListener
    void onDisconnected(SessionDisconnectEvent event) {
        openSessions.remove(event.getSessionId());
    }
}
