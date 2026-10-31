package com.ecommerce.notification.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class OrderCancelledConsumerTest {

    @Mock
    private NotificationProcessingService notificationProcessingService;

    private OrderCancelledConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new OrderCancelledConsumer(
                objectMapper, notificationProcessingService);
    }

    @Test
    void consume_shouldProcessOrderCancelledEvent() {
        String message = """
                {
                  "eventId": "cancel-event-test-001",
                  "eventType": "OrderCancelled",
                  "occurredAt": "2026-10-25T10:00:00Z",
                  "orderId": 101,
                  "customerId": 34
                }
                """;

        assertDoesNotThrow(() -> consumer.consume(message));

        verify(notificationProcessingService).process(
                "cancel-event-test-001",
                "OrderCancelled",
                "Order 101 cancelled for customer 34");
        verifyNoMoreInteractions(notificationProcessingService);
    }
}