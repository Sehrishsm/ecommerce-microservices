
package com.ecommerce.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record InventoryCreateRequest(

        @NotNull(message = "Product ID is required") @Positive(message = "Product ID must be positive") Long productId,

        @NotNull(message = "Initial quantity is required") @PositiveOrZero(message = "Initial quantity cannot be negative") Integer quantity) {
}
