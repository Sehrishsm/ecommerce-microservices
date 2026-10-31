
package com.ecommerce.order.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;

class OrderTest {

    @Test
    void newOrderShouldStartWithPendingStatusesAndZeroTotal() {
        Order order = new Order(10L);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(BigDecimal.ZERO, order.getTotalAmount());
        assertEquals(10L, order.getCustomerId());
    }

    @Test
    void addingItemsShouldCalculateOrderTotal() {
        Order order = new Order(10L);

        OrderItem firstItem = new OrderItem(
                1L, "Keyboard", new BigDecimal("25.50"), 2);

        OrderItem secondItem = new OrderItem(
                2L, "Mouse", new BigDecimal("10.00"), 1);

        order.addItem(firstItem);
        order.addItem(secondItem);

        assertEquals(new BigDecimal("61.00"), order.getTotalAmount());
        assertEquals(2, order.getItems().size());
        assertEquals(order, firstItem.getOrder());
        assertEquals(new BigDecimal("51.00"), firstItem.getLineTotal());
    }

    @Test
    void shouldConfirmOrderAfterSuccessfulPayment() {
        Order order = new Order(10L);

        order.beginPayment();
        order.recordPaymentSuccess();

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(PaymentStatus.SUCCESS, order.getPaymentStatus());
    }

    @Test
    void shouldMarkOrderPaymentFailedWhenPaymentFails() {
        Order order = new Order(10L);

        order.beginPayment();
        order.recordPaymentFailure();

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    void shouldCancelPendingOrder() {
        Order order = new Order(10L);

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void shouldAllowCancellationAfterPaymentFailure() {
        Order order = new Order(10L);

        order.beginPayment();
        order.recordPaymentFailure();
        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }

    @Test
    void shouldRejectPaymentBeforePaymentHasBegun() {
        Order order = new Order(10L);

        assertThrows(
                IllegalStateException.class,
                order::recordPaymentSuccess);
    }

    @Test
    void shouldRejectStartingPaymentTwice() {
        Order order = new Order(10L);

        order.beginPayment();

        assertThrows(
                IllegalStateException.class,
                order::beginPayment);
    }

    @Test
    void shouldRejectCancellationAfterOrderIsConfirmed() {
        Order order = new Order(10L);

        order.beginPayment();
        order.recordPaymentSuccess();

        assertThrows(
                IllegalStateException.class,
                order::cancel);
    }

    @Test
    void shouldRejectInvalidCustomerId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Order(0L));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Order(-1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Order(null));
    }

    @Test
    void shouldRejectNullOrderItem() {
        Order order = new Order(10L);

        assertThrows(
                IllegalArgumentException.class,
                () -> order.addItem(null));
    }

    @Test
    void shouldRejectInvalidOrderItemValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(
                        0L, "Keyboard", new BigDecimal("25.00"), 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(
                        1L, " ", new BigDecimal("25.00"), 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(
                        1L, "Keyboard", BigDecimal.ZERO, 1));

        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(
                        1L, "Keyboard", new BigDecimal("25.00"), 0));
    }

    @Test
    void shouldExposeItemsAsAnUnmodifiableList() {
        Order order = new Order(10L);

        order.addItem(new OrderItem(
                1L, "Keyboard", new BigDecimal("25.00"), 1));

        assertThrows(
                UnsupportedOperationException.class,
                () -> order.getItems().clear());

        assertEquals(1, order.getItems().size());
    }
}
