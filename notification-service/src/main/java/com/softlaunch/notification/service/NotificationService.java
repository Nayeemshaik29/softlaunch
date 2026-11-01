package com.softlaunch.notification.service;

import com.softlaunch.notification.dto.NotificationResponse;
import com.softlaunch.notification.event.MatchCreatedEvent;
import com.softlaunch.notification.event.MatchRemovedEvent;
import com.softlaunch.notification.event.MessageSentEvent;
import com.softlaunch.notification.model.Notification;
import com.softlaunch.notification.model.NotificationType;
import com.softlaunch.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String MATCH_MESSAGE = "You have a new match! 💘 Say hi before someone else does 😉";
    private static final String NEW_MESSAGE_TEXT = "💬 You have a new message";

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void handleMatchCreated(MatchCreatedEvent event) {
        notifyOnce(event.userAId(), event);
        notifyOnce(event.userBId(), event);
    }

    private void notifyOnce(UUID recipientId, MatchCreatedEvent event) {
        if (repository.existsBySourceEventIdAndRecipientId(event.eventId(), recipientId)) {
            log.info("Duplicate event {} for {}, skipping", event.eventId(), recipientId);
            return;
        }
        repository.save(new Notification(recipientId, NotificationType.MATCH_CREATED,
                MATCH_MESSAGE, event.matchId(), event.eventId()));
        log.info("Notified {} about match {}", recipientId, event.matchId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications(UUID me) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(me).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID me, UUID notificationId) {
        Notification notification = repository.findByIdAndRecipientId(notificationId, me)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void handleMatchRemoved(MatchRemovedEvent event) {
        long deleted = repository.deleteByReferenceIdAndType(event.matchId(), NotificationType.MATCH_CREATED)
                + repository.deleteByReferenceIdAndType(event.matchId(), NotificationType.NEW_MESSAGE);
        log.info("Match {} removed, deleted {} notification(s)", event.matchId(), deleted);
    }

    @Transactional
    public void handleMessageSent(MessageSentEvent event) {
        if (event.recipientOnline()) {
            log.info("Recipient {} is online, no notification needed", event.recipientId());
            return;
        }
        if (repository.existsBySourceEventIdAndRecipientId(event.eventId(), event.recipientId())) {
            log.info("Duplicate message.sent {}, skipping", event.eventId());
            return;
        }
        if (repository.existsByRecipientIdAndReferenceIdAndTypeAndReadFalse(
                event.recipientId(), event.matchId(), NotificationType.NEW_MESSAGE)) {
            log.info("Unread message notification already exists for chat {}, collapsing", event.matchId());
            return;
        }
        repository.save(new Notification(event.recipientId(), NotificationType.NEW_MESSAGE,
                NEW_MESSAGE_TEXT, event.matchId(), event.eventId()));
        log.info("Notified {} about a new message in chat {}", event.recipientId(), event.matchId());
    }
}