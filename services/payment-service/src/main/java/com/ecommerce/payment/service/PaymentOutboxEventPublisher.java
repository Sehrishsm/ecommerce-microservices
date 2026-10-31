
package com.ecommerce.payment.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecommerce.payment.entity.OutboxEvent;
import com.ecommerce.payment.event.PaymentEventProducer;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.event.PaymentSucceededEvent;
import com.ecommerce.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentOutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final ObjectMapper objectMapper;

    public PaymentOutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            PaymentEventProducer paymentEventProducer,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.paymentEventProducer = paymentEventProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING);

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                publishEvent(outboxEvent);

                outboxEvent.markPublished();
                outboxEventRepository.save(outboxEvent);

                logger.info(
                        "Payment outbox event published: eventId={}, eventType={}, paymentId={}",
                        outboxEvent.getEventId(),
                        outboxEvent.getEventType(),
                        outboxEvent.getAggregateId());

            } catch (Exception exception) {
                outboxEvent.recordFailure(exception.getMessage());
                outboxEventRepository.save(outboxEvent);

                logger.error(
                        "Payment outbox publication failed: eventId={}, eventType={}, attempts={}",
                        outboxEvent.getEventId(),
                        outboxEvent.getEventType(),
                        outboxEvent.getAttemptCount(),
                        exception);
            }
        }
    }

    private void publishEvent(OutboxEvent outboxEvent) throws Exception {
        switch (outboxEvent.getEventType()) {
            case "PaymentSucceeded" -> paymentEventProducer.publishPaymentSucceeded(
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            PaymentSucceededEvent.class));

            case "PaymentFailed" -> paymentEventProducer.publishPaymentFailed(
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            PaymentFailedEvent.class));

            default -> throw new IllegalArgumentException(
                    "Unsupported payment outbox event type: "
                            + outboxEvent.getEventType());
        }
    }
}
