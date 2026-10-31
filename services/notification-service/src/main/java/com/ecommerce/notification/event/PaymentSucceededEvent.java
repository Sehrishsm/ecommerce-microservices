package com.ecommerce.notification.event;

import java.time.Instant;

public record PaymentSucceededEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long paymentId,
        Long orderId,
        String amount,
        String currency) {
}