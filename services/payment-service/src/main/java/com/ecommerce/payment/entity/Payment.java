
package com.ecommerce.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    @NotNull
    @Positive
    @Column(nullable = false)
    private Long orderId;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    protected Payment() {
        // Required by JPA.
    }

    public Payment(Long orderId, BigDecimal amount, String idempotencyKey) {
        this.orderId = orderId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void markProcessing() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be processed");
        }

        this.status = PaymentStatus.PROCESSING;
    }

    public void markSuccess() {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only processing payments can succeed");
        }

        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        if (this.status != PaymentStatus.PENDING
                && this.status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Only pending or processing payments can fail");
        }

        this.status = PaymentStatus.FAILED;
    }
}