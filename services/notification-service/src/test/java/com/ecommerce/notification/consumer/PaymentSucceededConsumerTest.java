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
class PaymentSucceededConsumerTest {

    @Mock
    private NotificationProcessingService notificationProcessingService;

    private PaymentSucceededConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        consumer = new PaymentSucceededConsumer(
                objectMapper, notificationProcessingService);
    }

    @Test
    void consume_shouldProcessPaymentSucceededEvent() {
        String message = """
                {
                  "eventId": "payment-success-test-001",
                  "eventType": "PaymentSucceeded",
                  "occurredAt": "2026-10-25T10:00:00Z",
                  "paymentId": 501,
                  "orderId": 101,
                  "amount": "49.98",
                  "currency": "USD"
                }
                """;

        assertDoesNotThrow(() -> consumer.consume(message));

        verify(notificationProcessingService).process(
                "payment-success-test-001",
                "PaymentSucceeded",
                "Payment 501 succeeded for order 101, amount 49.98 USD");
        verifyNoMoreInteractions(notificationProcessingService);
    }
}