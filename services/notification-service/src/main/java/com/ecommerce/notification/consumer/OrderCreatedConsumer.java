
package com.ecommerce.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.notification.event.OrderCreatedEvent;
import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessingService notificationProcessingService;

    public OrderCreatedConsumer(
            ObjectMapper objectMapper,
            NotificationProcessingService notificationProcessingService) {
        this.objectMapper = objectMapper;
        this.notificationProcessingService = notificationProcessingService;
    }

    @KafkaListener(topics = "order.created")
    public void consume(String message) throws Exception {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

        notificationProcessingService.process(
                event.eventId(),
                event.eventType(),
                "Order " + event.orderId()
                        + " created for customer " + event.customerId()
                        + ", total " + event.totalAmount()
                        + " " + event.currency());
    }
}
