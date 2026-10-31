package com.ecommerce.auth.event;

import java.time.Instant;

public record UserRegisteredEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long userId,
        String email) {
}