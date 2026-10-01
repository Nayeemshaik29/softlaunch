package com.softlaunch.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "softlaunch.jwt")
public record JwtProperties(String secret, Duration expiration) {
}