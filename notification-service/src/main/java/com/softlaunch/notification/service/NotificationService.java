package com.softlaunch.notification.service;

import com.softlaunch.notification.dto.NotificationResponse;
import com.softlaunch.notification.event.MatchCreatedEvent;
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
}