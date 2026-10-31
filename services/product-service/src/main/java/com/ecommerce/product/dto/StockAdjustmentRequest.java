
package com.ecommerce.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockAdjustmentRequest(

        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be greater than zero") Integer quantity) {
}
