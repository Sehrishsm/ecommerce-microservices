package com.ecommerce.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.notification.event.PaymentFailedEvent;
import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentFailedConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessingService notificationProcessingService;

    public PaymentFailedConsumer(
            ObjectMapper objectMapper,
            NotificationProcessingService notificationProcessingService) {
        this.objectMapper = objectMapper;
        this.notificationProcessingService = notificationProcessingService;
    }

    @KafkaListener(topics = "payment.failed")
    public void consume(String message) throws Exception {
        PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);

        notificationProcessingService.process(
                event.eventId(),
                event.eventType(),
                "Payment " + event.paymentId()
                        + " failed for order " + event.orderId()
                        + ", amount " + event.amount()
                        + " " + event.currency());
    }
}