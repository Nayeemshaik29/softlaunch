package com.softlaunch.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "softlaunch.jwt")
public record JwtProperties(String secret) {
}