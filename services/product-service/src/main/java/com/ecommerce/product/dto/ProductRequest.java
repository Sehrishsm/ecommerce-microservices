
package com.ecommerce.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank(message = "Product name is required") @Size(max = 150, message = "Product name must not exceed 150 characters") String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,

        @NotNull(message = "Product price is required") @DecimalMin(value = "0.01", message = "Price must be greater than zero") @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places") BigDecimal price,

        @NotNull(message = "Category ID is required") @Positive(message = "Category ID must be positive") Long categoryId) {
}
