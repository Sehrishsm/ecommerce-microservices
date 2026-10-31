
package com.ecommerce.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderEventProducer;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    private ObjectMapper objectMapper;
    private OutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        publisher = new OutboxEventPublisher(
                outboxEventRepository,
                orderEventProducer,
                objectMapper);
    }

    @Test
    void publishPendingEventsShouldMarkEventPublished() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "event-created-201",
                "OrderCreated",
                Instant.parse("2026-10-28T12:00:00Z"),
                201L,
                10L,
                new BigDecimal("49.98"),
                "USD");

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-created-201",
                "Order",
                201L,
                "OrderCreated",
                "order.created",
                payload);

        when(outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        verify(orderEventProducer).publishOrderCreated(any(OrderCreatedEvent.class));
        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PUBLISHED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getPublishedAt());
        assertEquals(0, outboxEvent.getAttemptCount());
    }

    @Test
    void publishPendingEventsShouldRecordFailureAndKeepEventPending()
            throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent(
                "event-created-202",
                "OrderCreated",
                Instant.parse("2026-10-28T12:00:00Z"),
                202L,
                10L,
                new BigDecimal("25.00"),
                "USD");

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-created-202",
                "Order",
                202L,
                "OrderCreated",
                "order.created",
                payload);

        when(outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(orderEventProducer)
                .publishOrderCreated(any(OrderCreatedEvent.class));

        publisher.publishPendingEvents();

        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttemptCount());
        assertEquals("Kafka unavailable", outboxEvent.getLastError());
    }

    @Test
    void publishPendingEventsShouldRecordUnsupportedEventType() {
        OutboxEvent outboxEvent = new OutboxEvent(
                "event-unknown-203",
                "Order",
                203L,
                "UnknownEvent",
                "unknown.topic",
                "{}");

        when(outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        verify(orderEventProducer, never())
                .publishOrderCreated(any(OrderCreatedEvent.class));
        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttemptCount());
        assertNotNull(outboxEvent.getLastError());
    }
    @Test
    void publishPendingEventsShouldPublishCancelledEventAndMarkPublished()
            throws Exception {

        OrderCancelledEvent event = new OrderCancelledEvent(
                "event-cancelled-204",
                "OrderCancelled",
                Instant.parse("2026-10-30T12:00:00Z"),
                204L,
                10L);

        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-cancelled-204",
                "Order",
                204L,
                "OrderCancelled",
                "order.cancelled",
                payload);

        when(outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        verify(orderEventProducer).publishOrderCancelled(
                any(OrderCancelledEvent.class));
        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PUBLISHED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getPublishedAt());
        assertEquals(0, outboxEvent.getAttemptCount());
    }

    @Test
    void publishPendingEventsShouldDoNothingWhenNoPendingEventsExist() {

        when(outboxEventRepository
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING))
                .thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(outboxEventRepository)
                .findTop100ByStatusOrderByCreatedAtAsc(
                        OutboxEvent.Status.PENDING);

        verify(outboxEventRepository, never())
                .save(any(OutboxEvent.class));

        verifyNoInteractions(orderEventProducer);
    }
}
