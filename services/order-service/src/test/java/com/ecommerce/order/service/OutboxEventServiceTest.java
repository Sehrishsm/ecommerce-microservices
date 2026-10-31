package com.ecommerce.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.order.entity.OutboxEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

        @Mock
        private OutboxEventRepository outboxEventRepository;

        private ObjectMapper objectMapper;

        private OutboxEventService outboxEventService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper().findAndRegisterModules();
                outboxEventService = new OutboxEventService(
                                outboxEventRepository,
                                objectMapper);
        }

        @Test
        void saveOrderCreatedShouldPersistPendingOutboxEvent() throws Exception {
                OrderCreatedEvent event = new OrderCreatedEvent(
                                "event-101",
                                "OrderCreated",
                                Instant.parse("2026-10-28T12:00:00Z"),
                                101L,
                                10L,
                                new BigDecimal("49.98"),
                                "USD");

                when(outboxEventRepository.save(any(OutboxEvent.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                outboxEventService.saveOrderCreated(event);

                ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

                verify(outboxEventRepository).save(captor.capture());

                OutboxEvent savedEvent = captor.getValue();

                assertEquals("event-101", savedEvent.getEventId());
                assertEquals("Order", savedEvent.getAggregateType());
                assertEquals(101L, savedEvent.getAggregateId());
                assertEquals("OrderCreated", savedEvent.getEventType());
                assertEquals("order.created", savedEvent.getTopic());
                assertEquals(OutboxEvent.Status.PENDING, savedEvent.getStatus());
                assertEquals(0, savedEvent.getAttemptCount());
                assertNotNull(savedEvent.getPayload());
                assertEquals(49.98, objectMapper.readTree(savedEvent.getPayload())
                                .get("totalAmount").decimalValue().doubleValue(), 0.001);
        }
}