package com.softlaunch.safety.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param dailyReportLimit   max reports one user can send per day (stops "report bombing")
 * @param escalationReporters this many DIFFERENT reporters in the window = HIGH priority + under review
 * @param escalationWindow   how far back we count reporters
 * @param evidenceMessages   how many recent chat messages we copy into a report
 * @param retention          reports are deleted automatically after this (data-retention policy)
 * @param screenshotPeople   capturing chats of this many DIFFERENT people in the window = system report
 * @param screenshotWindow   how far back we look for the screenshot pattern
 * @param screenshotRetention how long one capture record is kept
 */
@ConfigurationProperties("softlaunch.safety")
public record SafetyProperties(int dailyReportLimit, int escalationReporters, Duration escalationWindow,
                               int evidenceMessages, Duration retention,
                               int screenshotPeople, Duration screenshotWindow, Duration screenshotRetention) { }
