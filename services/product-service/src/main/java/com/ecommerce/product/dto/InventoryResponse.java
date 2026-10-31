
package com.ecommerce.product.dto;

import com.ecommerce.product.entity.Inventory;

public record InventoryResponse(
        Long id,
        Long productId,
        Integer quantity,
        Long version) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getVersion());
    }
}
