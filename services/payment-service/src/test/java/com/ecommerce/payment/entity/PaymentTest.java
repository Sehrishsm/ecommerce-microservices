package com.ecommerce.payment.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void newPaymentStartsAsPending() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(1L, payment.getOrderId());
        assertEquals(
                new BigDecimal("49.98"),
                payment.getAmount());
    }

    @Test
    void pendingPaymentCanBecomeProcessing() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        payment.markProcessing();

        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    }

    @Test
    void processingPaymentCanBecomeSuccessful() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        payment.markProcessing();
        payment.markSuccess();

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
    }

    @Test
    void pendingPaymentCanBecomeFailed() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        payment.markFailed();

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void processingPaymentCanBecomeFailed() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        payment.markProcessing();
        payment.markFailed();

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void pendingPaymentCannotBecomeSuccessfulDirectly() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        assertThrows(
                IllegalStateException.class,
                payment::markSuccess);
    }

    @Test
    void successfulPaymentCannotBeFailed() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        payment.markProcessing();
        payment.markSuccess();

        assertThrows(
                IllegalStateException.class,
                payment::markFailed);
    }

    @Test
    void failedPaymentCannotBeProcessedAgain() {
        Payment payment = new Payment(
                1L,
                new BigDecimal("49.98"),
                "payment-request-1");

        payment.markFailed();

        assertThrows(
                IllegalStateException.class,
                payment::markProcessing);
    }
}