package com.softlaunch.safety.service;

import com.softlaunch.safety.client.ChatClient;
import com.softlaunch.safety.client.MatchClient;
import com.softlaunch.safety.config.SafetyProperties;
import com.softlaunch.safety.model.EvidenceMessage;
import com.softlaunch.safety.model.Report;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Copies the recent chat into the report, because chat messages delete themselves after 24h.
 * Rule: a report must NEVER fail just because evidence is missing. Safety first.
 */
@Component
public class EvidenceCollector {

    private static final Logger log = LoggerFactory.getLogger(EvidenceCollector.class);

    private final MatchClient matchClient;
    private final ChatClient chatClient;
    private final SafetyProperties props;

    public EvidenceCollector(MatchClient matchClient, ChatClient chatClient, SafetyProperties props) {
        this.matchClient = matchClient;
        this.chatClient = chatClient;
        this.props = props;
    }

    /** Throws 403 if the match exists but is NOT between these two people (privacy). */
    public void collect(Report report) {
        UUID matchId = report.getMatchId();
        if (matchId == null) {
            return;
        }
        MatchClient.MatchSummary match;
        try {
            match = matchClient.getMatch(matchId);
        } catch (FeignException.NotFound e) {
            report.evidenceUnavailable();      // already unmatched: chat is gone, still accept the report
            return;
        } catch (FeignException e) {
            log.warn("match-service unavailable while collecting evidence for match {}", matchId);
            report.evidenceUnavailable();
            return;
        }
        if (!match.isBetween(report.getReporterId(), report.getReportedUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only attach your own chats");
        }
        try {
            List<EvidenceMessage> copy = chatClient.recentMessages(matchId, props.evidenceMessages()).stream()
                    .map(m -> new EvidenceMessage(m.senderId(), m.text(), m.sentAt()))
                    .toList();
            report.attachEvidence(copy);
        } catch (FeignException e) {
            log.warn("chat-service unavailable while collecting evidence for match {}", matchId);
            report.evidenceUnavailable();
        }
    }
}
