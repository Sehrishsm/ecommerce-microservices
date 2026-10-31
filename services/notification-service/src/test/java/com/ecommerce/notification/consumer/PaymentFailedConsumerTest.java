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
class PaymentFailedConsumerTest {

    @Mock
    private NotificationProcessingService notificationProcessingService;

    private PaymentFailedConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        consumer = new PaymentFailedConsumer(
                objectMapper, notificationProcessingService);
    }

    @Test
    void consume_shouldProcessPaymentFailedEvent() {
        String message = """
                {
                  "eventId": "payment-failed-test-001",
                  "eventType": "PaymentFailed",
                  "occurredAt": "2026-10-25T10:05:00Z",
                  "paymentId": 502,
                  "orderId": 102,
                  "amount": "19.99",
                  "currency": "USD"
                }
                """;

        assertDoesNotThrow(() -> consumer.consume(message));

        verify(notificationProcessingService).process(
                "payment-failed-test-001",
                "PaymentFailed",
                "Payment 502 failed for order 102, amount 19.99 USD");
        verifyNoMoreInteractions(notificationProcessingService);
    }
}