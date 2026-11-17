package com.softlaunch.ai.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Ollama problems make ai-service DEGRADED, never DOWN: every AI feature has a fallback (templates,
 * rules, feed without vibe), so the service must keep receiving traffic. DOWN would make a load balancer
 * or Kubernetes pull it out of rotation and turn "worse answers" into "no answers".
 * Not part of the liveness/readiness groups (see config), only of the overall /actuator/health.
 */
@Component("ollama")
public class OllamaHealthIndicator implements HealthIndicator {

    public static final Status DEGRADED = new Status("DEGRADED", "AI unavailable: serving fallbacks");

    private final OllamaStatus status;

    public OllamaHealthIndicator(OllamaStatus status) {
        this.status = status;
    }

    @Override
    public Health health() {
        OllamaStatus.Snapshot s = status.snapshot();
        Health.Builder health = s.up() ? Health.up() : Health.status(DEGRADED).withDetail("problem", s.problem());
        if (!s.missingModels().isEmpty()) {
            health.withDetail("missingModels", s.missingModels());
        }
        return health.withDetail("checkedAt", s.checkedAt().toString()).build();
    }
}
