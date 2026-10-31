package com.ecommerce.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.notification.event.PaymentSucceededEvent;
import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentSucceededConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessingService notificationProcessingService;

    public PaymentSucceededConsumer(
            ObjectMapper objectMapper,
            NotificationProcessingService notificationProcessingService) {
        this.objectMapper = objectMapper;
        this.notificationProcessingService = notificationProcessingService;
    }

    @KafkaListener(topics = "payment.succeeded")
    public void consume(String message) throws Exception {
        PaymentSucceededEvent event = objectMapper.readValue(message, PaymentSucceededEvent.class);

        notificationProcessingService.process(
                event.eventId(),
                event.eventType(),
                "Payment " + event.paymentId()
                        + " succeeded for order " + event.orderId()
                        + ", amount " + event.amount()
                        + " " + event.currency());
    }
}