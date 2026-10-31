package com.ecommerce.product.dto;

public record InventoryReservationItem(
        Long productId,
        Integer quantity) {
}