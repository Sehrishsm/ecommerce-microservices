package com.ecommerce.notification.event;

import java.time.Instant;

public record OrderCancelledEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        Long customerId) {
}