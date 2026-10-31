package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "inventory_reservations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_inventory_reservation_order_product",
                columnNames = {"order_id", "product_id"}))
public class InventoryReservation {

    public enum Status {
        RESERVED,
        RELEASED,
        COMMITTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Order ID is required")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotNull(message = "Reservation quantity is required")
    @Min(value = 1, message = "Reservation quantity must be positive")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Reservation status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    protected InventoryReservation() {
        // Required by JPA
    }

    public InventoryReservation(Long orderId, Long productId, Integer quantity) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID must be positive");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be positive");
        }

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = Status.RESERVED;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Status getStatus() {
        return status;
    }

    public void release() {
        if (status == Status.COMMITTED) {
            throw new IllegalStateException(
                    "A committed inventory reservation cannot be released");
        }
        if (status == Status.RESERVED) {
            status = Status.RELEASED;
        }
    }

    public void commit() {
        if (status == Status.RELEASED) {
            throw new IllegalStateException(
                    "A released inventory reservation cannot be committed");
        }
        if (status == Status.RESERVED) {
            status = Status.COMMITTED;
        }
    }
}
