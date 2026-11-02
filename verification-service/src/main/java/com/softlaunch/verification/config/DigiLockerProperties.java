package com.softlaunch.verification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("softlaunch.kyc.digilocker")
public record DigiLockerProperties(String authorizeUrl, String tokenUrl, String clientId,
                                   String clientSecret, String redirectUri, String appReturnUrl) { }
