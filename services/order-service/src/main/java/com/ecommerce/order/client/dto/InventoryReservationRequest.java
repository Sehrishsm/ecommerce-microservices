package com.ecommerce.order.client.dto;

import java.util.List;

public record InventoryReservationRequest(
        Long orderId,
        List<InventoryReservationItem> items) {

    public record InventoryReservationItem(
            Long productId,
            Integer quantity) {
    }
}
