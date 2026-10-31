package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;

public class OrderResponse {

    private final Long id;
    private final Long customerId;
    private final OrderStatus status;
    private final PaymentStatus paymentStatus;
    private final Long paymentId;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;
    private final Long version;
    private final List<OrderItemResponse> items;

    public OrderResponse(
            Long id,
            Long customerId,
            OrderStatus status,
            PaymentStatus paymentStatus,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            Long version,
            List<OrderItemResponse> items) {

        this(
                id,
                customerId,
                status,
                paymentStatus,
                null,
                totalAmount,
                createdAt,
                version,
                items);
    }

    public OrderResponse(
            Long id,
            Long customerId,
            OrderStatus status,
            PaymentStatus paymentStatus,
            Long paymentId,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            Long version,
            List<OrderItemResponse> items) {

        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.paymentId = paymentId;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.version = version;
        this.items = List.copyOf(items);
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentId(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getVersion(),
                itemResponses);
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

    public List<OrderItemResponse> getItems() {
        return items;
    }
}
