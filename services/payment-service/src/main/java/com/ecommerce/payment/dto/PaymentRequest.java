package com.ecommerce.payment.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    @Positive(message = "Order ID must be positive")
    private Long orderId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be at least 0.01")
    @Digits(integer = 10, fraction = 2,
            message = "Payment amount must have at most 10 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    private String idempotencyKey;

    public PaymentRequest() {
        // Required for JSON deserialization.
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
