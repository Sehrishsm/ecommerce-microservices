
package com.ecommerce.order.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected OrderItem() {
        // Required by JPA.
    }

    public OrderItem(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity) {

        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "Product ID must be a positive number");
        }

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException(
                    "Product name is required");
        }

        if (unitPrice == null || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Unit price must be greater than zero");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }

        this.productId = productId;
        this.productName = productName.trim();
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    void setOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
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
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
