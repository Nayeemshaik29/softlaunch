package com.softlaunch.safety.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param dailyReportLimit   max reports one user can send per day (stops "report bombing")
 * @param escalationReporters this many DIFFERENT reporters in the window = HIGH priority + under review
 * @param escalationWindow   how far back we count reporters
 * @param evidenceMessages   how many recent chat messages we copy into a report
 * @param retention          reports are deleted automatically after this (data-retention policy)
 */
@ConfigurationProperties("softlaunch.safety")
public record SafetyProperties(int dailyReportLimit, int escalationReporters, Duration escalationWindow,
                               int evidenceMessages, Duration retention) { }
