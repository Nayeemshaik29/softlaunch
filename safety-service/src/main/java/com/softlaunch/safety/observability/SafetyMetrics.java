package com.softlaunch.safety.observability;

import com.softlaunch.safety.model.ReportPriority;
import com.softlaunch.safety.model.ReportReason;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business counters for trust & safety: reports (by reason + starting priority) and blocks.
 * A sudden jump in reports usually means a scam / spam wave, see the "Report spike" alert.
 * reason (9) x priority (3) = 27 series at most: small and fixed, so safe as tags.
 */
@Component
public class SafetyMetrics {

    private final MeterRegistry registry;
    private final Counter blocks;
    private final Counter unblocks;

    public SafetyMetrics(MeterRegistry registry) {
        this.registry = registry;
        // create the common series at 0 up front so the dashboard and rate() work before the first report
        for (ReportReason reason : ReportReason.values()) {
            report(reason, reason == ReportReason.SCREENSHOT_PATTERN ? ReportPriority.HIGH
                    : reason.isUrgent() ? ReportPriority.URGENT : ReportPriority.NORMAL);
        }
        blocks = Counter.builder("softlaunch.blocks").tag("action", "block").register(registry);
        unblocks = Counter.builder("softlaunch.blocks").tag("action", "unblock").register(registry);
    }

    private Counter report(ReportReason reason, ReportPriority priority) {
        return Counter.builder("softlaunch.reports")
                .description("Reports filed (by users, or by the system for screenshot patterns)")
                .tag("reason", reason.name())
                .tag("priority", priority.name())
                .register(registry);   // Micrometer returns the same counter for the same name + tags
    }

    public void reportFiled(ReportReason reason, ReportPriority priority) {
        report(reason, priority).increment();
    }

    public void blocked() {
        blocks.increment();
    }

    public void unblocked() {
        unblocks.increment();
    }
}
