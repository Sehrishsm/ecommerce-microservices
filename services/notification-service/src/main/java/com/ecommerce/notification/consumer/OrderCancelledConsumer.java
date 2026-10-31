package com.ecommerce.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.notification.event.OrderCancelledEvent;
import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderCancelledConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessingService notificationProcessingService;

    public OrderCancelledConsumer(
            ObjectMapper objectMapper,
            NotificationProcessingService notificationProcessingService) {
        this.objectMapper = objectMapper;
        this.notificationProcessingService = notificationProcessingService;
    }

    @KafkaListener(topics = "order.cancelled")
    public void consume(String message) throws Exception {
        OrderCancelledEvent event =
                objectMapper.readValue(message, OrderCancelledEvent.class);

        notificationProcessingService.process(
                event.eventId(),
                event.eventType(),
                "Order " + event.orderId()
                        + " cancelled for customer " + event.customerId());
    }
}