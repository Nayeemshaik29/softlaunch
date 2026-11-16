package com.softlaunch.ai.openers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param timeout  how long a user waits for the LLM before getting template openers instead
 * @param cacheTtl how long generated openers are reused for the same person + match (LLM calls are expensive)
 */
@ConfigurationProperties(prefix = "softlaunch.ai.openers")
public record AiProperties(Duration timeout, Duration cacheTtl) {

    public AiProperties {
        if (timeout == null) timeout = Duration.ofSeconds(8);
        if (cacheTtl == null) cacheTtl = Duration.ofMinutes(30);
    }
}
