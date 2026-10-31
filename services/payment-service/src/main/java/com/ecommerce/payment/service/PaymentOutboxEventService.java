package com.ecommerce.payment.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment.entity.OutboxEvent;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.event.PaymentSucceededEvent;
import com.ecommerce.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PaymentOutboxEventService {

    private static final String PAYMENT_SUCCEEDED_TOPIC = "payment.succeeded";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String CURRENCY = "USD";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentOutboxEventService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void savePaymentSucceeded(Payment payment) {
        String eventId = UUID.randomUUID().toString();

        PaymentSucceededEvent event = new PaymentSucceededEvent(
                eventId,
                "PaymentSucceeded",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount().toPlainString(),
                CURRENCY);

        saveEvent(
                eventId,
                payment,
                "PaymentSucceeded",
                PAYMENT_SUCCEEDED_TOPIC,
                event);
    }

    @Transactional
    public void savePaymentFailed(Payment payment) {
        String eventId = UUID.randomUUID().toString();

        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                "PaymentFailed",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount().toPlainString(),
                CURRENCY);

        saveEvent(
                eventId,
                payment,
                "PaymentFailed",
                PAYMENT_FAILED_TOPIC,
                event);
    }

    private void saveEvent(
            String eventId,
            Payment payment,
            String eventType,
            String topic,
            Object event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    eventId,
                    "Payment",
                    payment.getId(),
                    eventType,
                    topic,
                    payload);

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType
                            + " event for payment " + payment.getId(),
                    exception);
        }
    }
}
