package com.softlaunch.user.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Business counters for sign-ups and logins.
 * A spike in failed logins is the classic sign of password guessing (credential stuffing), see the alert in Grafana.
 * Never tag with email or user id: one time series per user would overload Prometheus.
 */
@Component
public class AuthMetrics {

    private final Counter signups;
    private final Counter loginSuccess;
    private final Counter loginFailed;
    private final Counter loginLocked;

    public AuthMetrics(MeterRegistry registry) {
        signups = Counter.builder("softlaunch.signups").description("New accounts").register(registry);
        loginSuccess = Counter.builder("softlaunch.logins").tag("result", "success").register(registry);
        loginFailed = Counter.builder("softlaunch.logins").tag("result", "failed").register(registry);
        loginLocked = Counter.builder("softlaunch.logins").tag("result", "locked").register(registry);
    }

    /** Counts the sign-up only once the DB transaction has really committed. */
    public void signupCommitted() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    signups.increment();
                }
            });
        } else {
            signups.increment();
        }
    }

    public void loginSucceeded() {
        loginSuccess.increment();
    }

    public void loginFailed() {
        loginFailed.increment();
    }

    /** Refused because the email is temporarily locked after too many wrong passwords. */
    public void loginLocked() {
        loginLocked.increment();
    }
}
