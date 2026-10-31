package com.ecommerce.order.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderEventProducer;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OutboxEventPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            OrderEventProducer orderEventProducer,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventProducer = orderEventProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:1000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents =
                outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING);

        for (OutboxEvent outboxEvent : pendingEvents) {
            try {
                publishEvent(outboxEvent);

                outboxEvent.markPublished();
                outboxEventRepository.save(outboxEvent);

                logger.info(
                        "Outbox event published: eventId={}, eventType={}, orderId={}",
                        outboxEvent.getEventId(),
                        outboxEvent.getEventType(),
                        outboxEvent.getAggregateId());

            } catch (Exception exception) {
                outboxEvent.recordFailure(exception.getMessage());
                outboxEventRepository.save(outboxEvent);

                logger.error(
                        "Outbox publication failed: eventId={}, eventType={}, attempts={}",
                        outboxEvent.getEventId(),
                        outboxEvent.getEventType(),
                        outboxEvent.getAttemptCount(),
                        exception);
            }
        }
    }

    private void publishEvent(OutboxEvent outboxEvent) throws Exception {
        switch (outboxEvent.getEventType()) {
            case "OrderCreated" -> orderEventProducer.publishOrderCreated(
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            OrderCreatedEvent.class));

            case "OrderCancelled" -> orderEventProducer.publishOrderCancelled(
                    objectMapper.readValue(
                            outboxEvent.getPayload(),
                            OrderCancelledEvent.class));

            default -> throw new IllegalArgumentException(
                    "Unsupported outbox event type: "
                            + outboxEvent.getEventType());
        }
    }
}
