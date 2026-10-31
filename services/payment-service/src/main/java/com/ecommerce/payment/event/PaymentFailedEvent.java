package com.ecommerce.payment.event;

import java.time.Instant;

public record PaymentFailedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long paymentId,
        Long orderId,
        String amount,
        String currency) {
}
