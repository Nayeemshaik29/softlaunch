package com.softlaunch.verification.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * KYC attempts as a Timer: one metric gives BOTH the count per outcome and how long our side took
 * (token exchange with DigiLocker + checks). The outcome tag is a short fixed code, never the
 * user-facing message, and never anything personal (no name, DOB, DigiLocker id).
 */
@Component
public class KycMetrics {

    public static final String VERIFIED = "verified";
    public static final String CANCELLED = "cancelled";
    public static final String PROVIDER_ERROR = "provider_error";
    public static final String UNDERAGE = "underage";
    public static final String DUPLICATE_IDENTITY = "duplicate_identity";
    public static final String PROFILE_UNAVAILABLE = "profile_unavailable";
    public static final String DOB_MISMATCH = "dob_mismatch";

    private final MeterRegistry registry;

    public KycMetrics(MeterRegistry registry) {
        this.registry = registry;
        List.of(VERIFIED, CANCELLED, PROVIDER_ERROR, UNDERAGE, DUPLICATE_IDENTITY, PROFILE_UNAVAILABLE, DOB_MISMATCH)
                .forEach(this::timer);   // create every series at 0 so the dashboard works before the first attempt
    }

    private Timer timer(String outcome) {
        return Timer.builder("softlaunch.kyc")
                .description("KYC (DigiLocker) completions by outcome")
                .tag("outcome", outcome)
                .register(registry);
    }

    public void record(String outcome, long startedNanos) {
        timer(outcome).record(Duration.ofNanos(System.nanoTime() - startedNanos));
    }
}
