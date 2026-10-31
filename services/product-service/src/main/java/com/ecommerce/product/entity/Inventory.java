
package com.ecommerce.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Version
    private Long version;

    protected Inventory() {
        // Required by JPA
    }

    public Inventory(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
