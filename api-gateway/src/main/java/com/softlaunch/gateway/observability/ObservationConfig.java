package com.softlaunch.gateway.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

/**
 * Skips traces for background "plumbing" calls so real user traces are easy to find:
 * /actuator (Prometheus scrapes every 15s) and /eureka (registry heartbeats every 30s).
 */
@Configuration(proxyBeanMethods = false)
class ObservationConfig {

    @Bean
    ObservationPredicate skipPlumbingObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext request) {
                return !isPlumbing(request.getCarrier().getPath().value());
            }
            if (context instanceof ClientRequestObservationContext client && client.getCarrier() != null) {
                return !isPlumbing(client.getCarrier().getURI().getPath());
            }
            return true;
        };
    }

    private static boolean isPlumbing(String path) {
        return path != null && (path.startsWith("/actuator") || path.startsWith("/eureka"));
    }
}
