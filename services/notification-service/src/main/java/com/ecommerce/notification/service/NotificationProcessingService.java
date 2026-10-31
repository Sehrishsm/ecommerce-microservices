
package com.ecommerce.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.notification.entity.ProcessedEvent;
import com.ecommerce.notification.repository.ProcessedEventRepository;

@Service
public class NotificationProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessingService.class);

    private final ProcessedEventRepository processedEventRepository;

    public NotificationProcessingService(
            ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean process(
            String eventId,
            String eventType,
            String notificationMessage) {

        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException(
                    "Event ID must not be blank");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Event type must not be blank");
        }

        if (notificationMessage == null || notificationMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "Notification message must not be blank");
        }

        if (processedEventRepository.existsByEventId(eventId)) {
            log.info(
                    "Skipping duplicate event: eventId={}, eventType={}",
                    eventId,
                    eventType);
            return false;
        }

        processedEventRepository.saveAndFlush(
                new ProcessedEvent(eventId, eventType));

        log.info(
                "SIMULATED NOTIFICATION: eventId={}, eventType={}, message={}",
                eventId,
                eventType,
                notificationMessage);

        return true;
    }
}
