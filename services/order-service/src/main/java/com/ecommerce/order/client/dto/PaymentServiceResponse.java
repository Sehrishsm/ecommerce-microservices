
package com.ecommerce.order.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentServiceResponse(
        Long id,
        String idempotencyKey,
        Long orderId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {
}
