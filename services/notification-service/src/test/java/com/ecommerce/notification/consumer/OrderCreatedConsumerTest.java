
package com.ecommerce.notification.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private NotificationProcessingService notificationProcessingService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeAndDelegateOrderCreatedEvent() throws Exception {
        OrderCreatedConsumer consumer = new OrderCreatedConsumer(objectMapper, notificationProcessingService);

        String json = """
                {
                    "eventId": "event-test-001",
                    "eventType": "OrderCreated",
                    "occurredAt": "2026-10-24T06:00:00Z",
                    "orderId": 101,
                    "customerId": 34,
                    "totalAmount": 149.99,
                    "currency": "USD"
                }
                """;

        assertDoesNotThrow(() -> consumer.consume(json));

        verify(notificationProcessingService).process(
                eq("event-test-001"),
                eq("OrderCreated"),
                eq("Order 101 created for customer 34, total 149.99 USD"));

        verifyNoMoreInteractions(notificationProcessingService);
    }
}
