package com.ecommerce.notification.event;

import java.time.Instant;

public record UserRegisteredEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long userId,
        String email) {
}