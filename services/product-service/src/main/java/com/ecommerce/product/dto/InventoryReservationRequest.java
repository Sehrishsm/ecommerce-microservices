package com.ecommerce.product.dto;

import java.util.List;

public record InventoryReservationRequest(
Long orderId,
List<InventoryReservationItem> items) {
}
