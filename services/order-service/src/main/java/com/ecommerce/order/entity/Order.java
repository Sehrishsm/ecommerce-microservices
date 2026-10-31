package com.ecommerce.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
        // Required by JPA.
    }

    public Order(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException(
                    "Customer ID must be a positive number");
        }

        this.customerId = customerId;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException(
                    "Order item cannot be null");
        }

        item.setOrder(this);
        items.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        totalAmount = items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void beginPayment() {
        requireStatus(OrderStatus.PENDING,
                "Only a pending order can begin payment");

        status = OrderStatus.PAYMENT_PENDING;
    }

    public void associatePayment(Long paymentId) {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException(
                    "Payment ID must be a positive number");
        }

        requireStatus(OrderStatus.PAYMENT_PENDING,
                "Payment can only be associated with an order awaiting payment");

        if (this.paymentId != null) {
            if (this.paymentId.equals(paymentId)) {
                return;
            }

            throw new IllegalStateException(
                    "A different payment is already associated with this order");
        }

        this.paymentId = paymentId;
    }

    public void recordPaymentSuccess() {
        requireStatus(OrderStatus.PAYMENT_PENDING,
                "Payment can succeed only while payment is pending");

        if (paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment result has already been recorded");
        }

        paymentStatus = PaymentStatus.SUCCESS;
        status = OrderStatus.CONFIRMED;
    }

    public void recordPaymentFailure() {
        requireStatus(OrderStatus.PAYMENT_PENDING,
                "Payment can fail only while payment is pending");

        if (paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment result has already been recorded");
        }

        paymentStatus = PaymentStatus.FAILED;
        status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        if (status != OrderStatus.PENDING
                && status != OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException(
                    "Only a pending or payment-failed order can be cancelled");
        }

        status = OrderStatus.CANCELLED;
    }

    private void requireStatus(
            OrderStatus expectedStatus,
            String message) {
        if (status != expectedStatus) {
            throw new IllegalStateException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
