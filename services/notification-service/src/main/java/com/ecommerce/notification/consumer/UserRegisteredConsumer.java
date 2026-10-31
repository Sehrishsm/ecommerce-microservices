package com.ecommerce.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.notification.event.UserRegisteredEvent;
import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class UserRegisteredConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessingService notificationProcessingService;

    public UserRegisteredConsumer(
            ObjectMapper objectMapper,
            NotificationProcessingService notificationProcessingService) {
        this.objectMapper = objectMapper;
        this.notificationProcessingService = notificationProcessingService;
    }

    @KafkaListener(topics = "user.registered")
    public void consume(String message) throws Exception {
        UserRegisteredEvent event =
                objectMapper.readValue(message, UserRegisteredEvent.class);

        notificationProcessingService.process(
                event.eventId(),
                event.eventType(),
                "User registered: user " + event.userId()
                        + ", email " + event.email());
    }
}