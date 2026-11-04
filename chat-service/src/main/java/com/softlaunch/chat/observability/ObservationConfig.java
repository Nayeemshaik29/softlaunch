package com.softlaunch.chat.observability;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Skips traces for background "plumbing" calls so real user traces are easy to find:
 * /actuator (Prometheus scrapes every 15s, health checks) and /eureka (registry heartbeats every 30s).
 */
@Configuration(proxyBeanMethods = false)
class ObservationConfig {

    @Bean
    ObservationPredicate skipActuatorObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext request) {
                return !isPlumbing(request.getCarrier().getRequestURI());
            }
            if (context instanceof ClientRequestObservationContext client && client.getCarrier() != null) {
                return !isPlumbing(client.getCarrier().getURI().getPath());
            }
            // calls made while serving /actuator (e.g. a health check pinging Redis) are skipped too
            return !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                    && isPlumbing(attrs.getRequest().getRequestURI()));
        };
    }

    private static boolean isPlumbing(String path) {
        return path != null && (path.startsWith("/actuator") || path.startsWith("/eureka"));
    }
}
