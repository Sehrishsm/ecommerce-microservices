package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderItem;

import java.math.BigDecimal;

public class OrderItemResponse {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final int quantity;
    private final BigDecimal lineTotal;

    public OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
