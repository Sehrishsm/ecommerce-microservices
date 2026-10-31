package com.ecommerce.product.dto;

import com.ecommerce.product.entity.InventoryReservation;

public record InventoryReservationResponse(
Long id,
Long orderId,
Long productId,
Integer quantity,
InventoryReservation.Status status) {

public static InventoryReservationResponse from(
        InventoryReservation reservation) {
    return new InventoryReservationResponse(
            reservation.getId(),
            reservation.getOrderId(),
            reservation.getProductId(),
            reservation.getQuantity(),
            reservation.getStatus());
}

}
