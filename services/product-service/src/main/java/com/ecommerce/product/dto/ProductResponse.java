
package com.ecommerce.product.dto;

import java.math.BigDecimal;

import com.ecommerce.product.entity.Product;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName());
    }
}
