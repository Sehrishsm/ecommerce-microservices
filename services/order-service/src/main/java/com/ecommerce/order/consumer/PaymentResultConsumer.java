package com.ecommerce.order.consumer;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;
import com.ecommerce.order.event.PaymentFailedEvent;
import com.ecommerce.order.event.PaymentSucceededEvent;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentResultConsumer {

        private static final Logger log = LoggerFactory.getLogger(PaymentResultConsumer.class);

        private final ObjectMapper objectMapper;
        private final OrderService orderService;
        private final InventoryServiceClient inventoryServiceClient;

        public PaymentResultConsumer(
                        ObjectMapper objectMapper,
                        OrderService orderService,
                        InventoryServiceClient inventoryServiceClient) {
                this.objectMapper = objectMapper;
                this.orderService = orderService;
                this.inventoryServiceClient = inventoryServiceClient;
        }

        @KafkaListener(topics = "payment.succeeded", groupId = "order-service-group")
        public void consumePaymentSucceeded(String message) throws Exception {
                PaymentSucceededEvent event = objectMapper.readValue(message, PaymentSucceededEvent.class);

                validateEvent(
                                event.eventId(),
                                event.eventType(),
                                "PaymentSucceeded",
                                event.orderId(),
                                event.paymentId());

                OrderResponse order = orderService.getOrderById(event.orderId());
                validatePaymentId(event.paymentId(), order);
                validatePaymentDetails(event.amount(), event.currency(), order);

                // Commit all reservations before recording the terminal order state.
                // If a call fails, Kafka can retry the event.
                commitInventory(order);

                if (order.getStatus() == OrderStatus.CONFIRMED
                                && order.getPaymentStatus() == PaymentStatus.SUCCESS) {
                        log.info(
                                        "Duplicate payment success verified; inventory is committed: eventId={}, orderId={}",
                                        event.eventId(), event.orderId());
                        return;
                }

                orderService.recordPaymentSuccess(event.orderId());

                log.info(
                                "Order confirmed after payment success: eventId={}, orderId={}",
                                event.eventId(), event.orderId());
        }

        @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
        public void consumePaymentFailed(String message) throws Exception {
                PaymentFailedEvent event = objectMapper.readValue(message, PaymentFailedEvent.class);

                validateEvent(
                                event.eventId(),
                                event.eventType(),
                                "PaymentFailed",
                                event.orderId(),
                                event.paymentId());

                OrderResponse order = orderService.getOrderById(event.orderId());
                validatePaymentId(event.paymentId(), order);
                validatePaymentDetails(event.amount(), event.currency(), order);
                // Release all reservations before recording payment failure.
                // A retry can safely repeat reservations already released.
                releaseInventory(order);

                if (order.getStatus() == OrderStatus.PAYMENT_FAILED
                                && order.getPaymentStatus() == PaymentStatus.FAILED) {
                        log.info(
                                        "Duplicate payment failure verified; inventory is released: eventId={}, orderId={}",
                                        event.eventId(), event.orderId());
                        return;
                }

                orderService.recordPaymentFailure(event.orderId());

                log.info(
                                "Order marked payment-failed: eventId={}, orderId={}",
                                event.eventId(), event.orderId());
        }

        private void commitInventory(OrderResponse order) {
                for (var item : order.getItems()) {
                        inventoryServiceClient.commitReservation(
                                        order.getId(), item.getProductId());
                }
        }

        private void releaseInventory(OrderResponse order) {
                for (var item : order.getItems()) {
                        inventoryServiceClient.releaseReservation(
                                        order.getId(), item.getProductId());
                }
        }

        private void validatePaymentDetails(
                        String amount,
                        String currency,
                        OrderResponse order) {

                if (amount == null || amount.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Payment event amount must not be blank");
                }

                BigDecimal eventAmount;
                try {
                        eventAmount = new BigDecimal(amount);
                } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException(
                                        "Payment event amount must be a valid number", ex);
                }

                if (eventAmount.compareTo(order.getTotalAmount()) != 0) {
                        throw new IllegalArgumentException(
                                        "Payment event amount does not match order total");
                }

                if (!"USD".equals(currency)) {
                        throw new IllegalArgumentException(
                                        "Unsupported payment currency: " + currency);
                }
        }

        private void validateEvent(
                        String eventId,
                        String eventType,
                        String expectedType,
                        Long orderId,
                        Long paymentId) {

                if (eventId == null || eventId.isBlank()) {
                        throw new IllegalArgumentException("Event ID must not be blank");
                }

                if (!expectedType.equals(eventType)) {
                        throw new IllegalArgumentException(
                                        "Unexpected event type: " + eventType);
                }

                if (orderId == null || orderId <= 0) {
                        throw new IllegalArgumentException(
                                        "Event order ID must be a positive number");
                }

                if (paymentId == null || paymentId <= 0) {
                        throw new IllegalArgumentException(
                                        "Event payment ID must be a positive number");
                }
        }

        private void validatePaymentId(Long eventPaymentId, OrderResponse order) {
                if (order.getPaymentId() == null
                                || !order.getPaymentId().equals(eventPaymentId)) {
                        throw new IllegalArgumentException(
                                        "Payment event ID does not match order payment ID");
                }
        }
}
