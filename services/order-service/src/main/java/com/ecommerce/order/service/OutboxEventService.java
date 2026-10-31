package com.ecommerce.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OutboxEventService {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveOrderCreated(OrderCreatedEvent event) {
        saveEvent(
                event.eventId(),
                "Order",
                event.orderId(),
                event.eventType(),
                ORDER_CREATED_TOPIC,
                event);
    }

    @Transactional
    public void saveOrderCancelled(OrderCancelledEvent event) {
        saveEvent(
                event.eventId(),
                "Order",
                event.orderId(),
                event.eventType(),
                ORDER_CANCELLED_TOPIC,
                event);
    }

    private void saveEvent(
            String eventId,
            String aggregateType,
            Long aggregateId,
            String eventType,
            String topic,
            Object event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    eventId,
                    aggregateType,
                    aggregateId,
                    eventType,
                    topic,
                    payload);

            outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize outbox event: " + eventType,
                    exception);
        }
    }
}
