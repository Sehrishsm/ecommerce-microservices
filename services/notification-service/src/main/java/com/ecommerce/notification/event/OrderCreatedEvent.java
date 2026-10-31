
package com.ecommerce.notification.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long orderId,
        Long customerId,
        BigDecimal totalAmount,
        String currency) {
}
