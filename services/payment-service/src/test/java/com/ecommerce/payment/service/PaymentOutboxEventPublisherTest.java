package com.ecommerce.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.payment.entity.OutboxEvent;
import com.ecommerce.payment.event.PaymentEventProducer;
import com.ecommerce.payment.event.PaymentFailedEvent;
import com.ecommerce.payment.event.PaymentSucceededEvent;
import com.ecommerce.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    private ObjectMapper objectMapper;
    private PaymentOutboxEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        publisher = new PaymentOutboxEventPublisher(
                outboxEventRepository,
                paymentEventProducer,
                objectMapper);
    }

    @Test
    void publishPendingEventsShouldPublishSucceededEventAndMarkPublished()
            throws Exception {

        PaymentSucceededEvent paymentEvent = new PaymentSucceededEvent(
                "event-payment-success-1",
                "PaymentSucceeded",
                Instant.parse("2026-10-30T10:00:00Z"),
                101L,
                1001L,
                "49.99",
                "USD");

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-payment-success-1",
                "Payment",
                101L,
                "PaymentSucceeded",
                "payment.succeeded",
                objectMapper.writeValueAsString(paymentEvent));

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        verify(paymentEventProducer).publishPaymentSucceeded(
                any(PaymentSucceededEvent.class));

        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PUBLISHED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getPublishedAt());
        assertEquals(0, outboxEvent.getAttemptCount());
    }

    @Test
    void publishPendingEventsShouldPublishFailedEventAndMarkPublished()
            throws Exception {

        PaymentFailedEvent paymentEvent = new PaymentFailedEvent(
                "event-payment-failed-1",
                "PaymentFailed",
                Instant.parse("2026-10-30T10:05:00Z"),
                102L,
                1002L,
                "25.00",
                "USD");

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-payment-failed-1",
                "Payment",
                102L,
                "PaymentFailed",
                "payment.failed",
                objectMapper.writeValueAsString(paymentEvent));

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        verify(paymentEventProducer).publishPaymentFailed(
                any(PaymentFailedEvent.class));

        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PUBLISHED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getPublishedAt());
        assertEquals(0, outboxEvent.getAttemptCount());
    }

    @Test
    void publishPendingEventsShouldRecordFailureWhenPublishingThrows()
            throws Exception {

        PaymentSucceededEvent paymentEvent = new PaymentSucceededEvent(
                "event-payment-success-2",
                "PaymentSucceeded",
                Instant.parse("2026-10-30T10:10:00Z"),
                103L,
                1003L,
                "75.00",
                "USD");

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-payment-success-2",
                "Payment",
                103L,
                "PaymentSucceeded",
                "payment.succeeded",
                objectMapper.writeValueAsString(paymentEvent));

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(paymentEventProducer)
                .publishPaymentSucceeded(any(PaymentSucceededEvent.class));

        publisher.publishPendingEvents();

        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttemptCount());
        assertEquals("Kafka unavailable", outboxEvent.getLastError());
        assertNull(outboxEvent.getPublishedAt());
    }

    @Test
    void publishPendingEventsShouldRecordFailureForUnsupportedEventType() {

        OutboxEvent outboxEvent = new OutboxEvent(
                "event-payment-unknown-1",
                "Payment",
                104L,
                "UnsupportedEvent",
                "payment.unknown",
                "{}");

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING))
                .thenReturn(List.of(outboxEvent));

        publisher.publishPendingEvents();

        verifyNoInteractions(paymentEventProducer);
        verify(outboxEventRepository).save(outboxEvent);

        assertEquals(OutboxEvent.Status.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getAttemptCount());
        assertNotNull(outboxEvent.getLastError());
        assertTrue(outboxEvent.getLastError().contains(
                "Unsupported payment outbox event type"));
    }

    @Test
    void publishPendingEventsShouldDoNothingWhenNoPendingEventsExist() {

        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING))
                .thenReturn(List.of());

        publisher.publishPendingEvents();

        verify(outboxEventRepository).findTop100ByStatusOrderByCreatedAtAsc(
                OutboxEvent.Status.PENDING);

        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        verifyNoInteractions(paymentEventProducer);
    }
}