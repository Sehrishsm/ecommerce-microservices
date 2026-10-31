package com.ecommerce.order.client.dto;

public record InventoryReservationResponse(
        Long id,
        Long orderId,
        Long productId,
        Integer quantity,
        String status) {
}
