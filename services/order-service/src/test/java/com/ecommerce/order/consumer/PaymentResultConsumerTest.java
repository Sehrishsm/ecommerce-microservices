package com.ecommerce.order.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class PaymentResultConsumerTest {

        @Mock
        private OrderService orderService;

        @Mock
        private InventoryServiceClient inventoryServiceClient;

        private PaymentResultConsumer consumer;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper()
                                .registerModule(new JavaTimeModule());

                consumer = new PaymentResultConsumer(
                                objectMapper, orderService, inventoryServiceClient);
        }

        @Test
        void shouldCommitInventoryAndConfirmOrderWhenPaymentSucceeds()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                consumer.consumePaymentSucceeded(successEvent("event-success-101", 101L));

                verify(orderService).getOrderById(101L);
                verify(inventoryServiceClient).commitReservation(101L, 1L);
                verify(orderService).recordPaymentSuccess(101L);
        }

        @Test
        void shouldReleaseInventoryAndMarkOrderFailedWhenPaymentFails()
                        throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, 502L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                consumer.consumePaymentFailed(failureEvent("event-failure-102", 102L));

                verify(orderService).getOrderById(102L);
                verify(inventoryServiceClient).releaseReservation(102L, 1L);
                verify(orderService).recordPaymentFailure(102L);
        }

        @Test
        void shouldRejectSuccessfulPaymentWhenOrderHasNoStoredPaymentId()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, null, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(
                                                successEvent("event-success-missing-payment-id", 101L)));

                verify(inventoryServiceClient, never()).commitReservation(101L, 1L);
                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldRejectSuccessfulPaymentWhenPaymentIdDoesNotMatchOrder()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 999L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(
                                                successEvent("event-success-wrong-payment-id", 101L)));

                verify(inventoryServiceClient, never()).commitReservation(101L, 1L);
                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldRejectFailedPaymentWhenOrderHasNoStoredPaymentId()
                        throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, null, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentFailed(
                                                failureEvent("event-failure-missing-payment-id", 102L)));

                verify(inventoryServiceClient, never()).releaseReservation(102L, 1L);
                verify(orderService, never()).recordPaymentFailure(102L);
        }

        @Test
        void shouldRejectFailedPaymentWhenPaymentIdDoesNotMatchOrder()
                        throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, 999L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentFailed(
                                                failureEvent("event-failure-wrong-payment-id", 102L)));

                verify(inventoryServiceClient, never()).releaseReservation(102L, 1L);
                verify(orderService, never()).recordPaymentFailure(102L);
        }

        @Test
        void shouldRetryInventoryCommitButNotConfirmDuplicateSuccess()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.CONFIRMED, PaymentStatus.SUCCESS));

                assertDoesNotThrow(() -> consumer.consumePaymentSucceeded(
                                successEvent("event-success-duplicate", 101L)));

                verify(orderService).getOrderById(101L);
                verify(inventoryServiceClient).commitReservation(101L, 1L);
                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldRetryInventoryReleaseButNotRecordDuplicateFailure()
                        throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, 502L, OrderStatus.PAYMENT_FAILED, PaymentStatus.FAILED));

                assertDoesNotThrow(() -> consumer.consumePaymentFailed(
                                failureEvent("event-failure-duplicate", 102L)));

                verify(orderService).getOrderById(102L);
                verify(inventoryServiceClient).releaseReservation(102L, 1L);
                verify(orderService, never()).recordPaymentFailure(102L);
        }

        @Test
        void shouldPropagateInventoryCommitFailureWithoutConfirmingOrder()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                org.mockito.Mockito.doThrow(new IllegalStateException("Product Service unavailable"))
                                .when(inventoryServiceClient).commitReservation(101L, 1L);

                assertThrows(IllegalStateException.class,
                                () -> consumer.consumePaymentSucceeded(
                                                successEvent("event-success-retry", 101L)));

                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldPropagateInventoryReleaseFailureWithoutMarkingOrderFailed()
                        throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, 502L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                org.mockito.Mockito.doThrow(new IllegalStateException("Product Service unavailable"))
                                .when(inventoryServiceClient).releaseReservation(102L, 1L);

                assertThrows(IllegalStateException.class,
                                () -> consumer.consumePaymentFailed(
                                                failureEvent("event-failure-retry", 102L)));

                verify(orderService, never()).recordPaymentFailure(102L);
        }

        @Test
        void shouldRejectMismatchedAmountForSuccessfulPayment()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                String json = """
                                {
                                    "eventId": "event-wrong-amount-success",
                                    "eventType": "PaymentSucceeded",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 501,
                                    "orderId": 101,
                                    "amount": "24.99",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verify(inventoryServiceClient, never()).commitReservation(101L, 1L);
                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldRejectMismatchedAmountForFailedPayment()
                        throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, 502L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                String json = """
                                {
                                    "eventId": "event-wrong-amount-failure",
                                    "eventType": "PaymentFailed",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 502,
                                    "orderId": 102,
                                    "amount": "25.00",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentFailed(json));

                verify(inventoryServiceClient, never()).releaseReservation(102L, 1L);
                verify(orderService, never()).recordPaymentFailure(102L);
        }

        @Test
        void shouldRejectUnsupportedCurrencyBeforeCommittingInventory()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                String json = """
                                {
                                    "eventId": "event-wrong-currency",
                                    "eventType": "PaymentSucceeded",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 501,
                                    "orderId": 101,
                                    "amount": "49.98",
                                    "currency": "EUR"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verify(inventoryServiceClient, never()).commitReservation(101L, 1L);
                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldRejectMalformedAmountBeforeChangingOrderOrInventory()
                        throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                String json = """
                                {
                                    "eventId": "event-invalid-amount",
                                    "eventType": "PaymentSucceeded",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 501,
                                    "orderId": 101,
                                    "amount": "not-a-number",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verify(inventoryServiceClient, never()).commitReservation(101L, 1L);
                verify(orderService, never()).recordPaymentSuccess(101L);
        }

        @Test
        void shouldRejectBlankEventId() {
                String json = """
                                {
                                    "eventId": " ",
                                    "eventType": "PaymentSucceeded",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 501,
                                    "orderId": 101,
                                    "amount": "49.98",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verifyNoInteractions(orderService, inventoryServiceClient);
        }

        @Test
        void shouldRejectUnexpectedEventType() {
                String json = """
                                {
                                    "eventId": "event-invalid-type",
                                    "eventType": "PaymentFailed",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 501,
                                    "orderId": 101,
                                    "amount": "49.98",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verifyNoInteractions(orderService, inventoryServiceClient);
        }

        @Test
        void shouldRejectInvalidOrderId() {
                String json = """
                                {
                                    "eventId": "event-invalid-order",
                                    "eventType": "PaymentSucceeded",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "paymentId": 501,
                                    "orderId": 0,
                                    "amount": "49.98",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verifyNoInteractions(orderService, inventoryServiceClient);
        }

        @Test
        void shouldRejectMissingPaymentId() {
                String json = """
                                {
                                    "eventId": "event-missing-payment-id",
                                    "eventType": "PaymentSucceeded",
                                    "occurredAt": "2026-10-25T10:00:00Z",
                                    "orderId": 101,
                                    "amount": "49.98",
                                    "currency": "USD"
                                }
                                """;

                assertThrows(IllegalArgumentException.class,
                                () -> consumer.consumePaymentSucceeded(json));

                verifyNoInteractions(orderService, inventoryServiceClient);
        }

        @Test
        void shouldRejectMalformedJson() {
                assertThrows(Exception.class,
                                () -> consumer.consumePaymentSucceeded("{invalid-json"));

                verifyNoInteractions(orderService, inventoryServiceClient);
        }

        @Test
        void shouldPropagateOrderConfirmationFailureAfterInventoryCommit() throws Exception {
                when(orderService.getOrderById(101L))
                                .thenReturn(orderResponse(
                                                101L, 501L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                org.mockito.Mockito.doThrow(new IllegalStateException("Order confirmation failed"))
                                .when(orderService).recordPaymentSuccess(101L);

                assertThrows(
                                IllegalStateException.class,
                                () -> consumer.consumePaymentSucceeded(
                                                successEvent("event-success-order-update-fails", 101L)));

                verify(inventoryServiceClient).commitReservation(101L, 1L);
                verify(orderService).recordPaymentSuccess(101L);
        }

        @Test
        void shouldPropagateOrderFailureUpdateAfterInventoryRelease() throws Exception {
                when(orderService.getOrderById(102L))
                                .thenReturn(orderResponse(
                                                102L, 502L, OrderStatus.PAYMENT_PENDING, PaymentStatus.PENDING));

                org.mockito.Mockito.doThrow(new IllegalStateException("Order failure update failed"))
                                .when(orderService).recordPaymentFailure(102L);

                assertThrows(
                                IllegalStateException.class,
                                () -> consumer.consumePaymentFailed(
                                                failureEvent("event-failure-order-update-fails", 102L)));

                verify(inventoryServiceClient).releaseReservation(102L, 1L);
                verify(orderService).recordPaymentFailure(102L);
        }

        private String successEvent(String eventId, Long orderId) throws Exception {
                return objectMapper.writeValueAsString(
                                new TestPaymentEvent(
                                                eventId, "PaymentSucceeded",
                                                Instant.parse("2026-10-25T10:00:00Z"),
                                                501L, orderId, "49.98", "USD"));
        }

        private String failureEvent(String eventId, Long orderId) throws Exception {
                return objectMapper.writeValueAsString(
                                new TestPaymentEvent(
                                                eventId, "PaymentFailed",
                                                Instant.parse("2026-10-25T10:00:00Z"),
                                                502L, orderId, "49.98", "USD"));
        }

        private OrderResponse orderResponse(
                        Long id,
                        Long paymentId,
                        OrderStatus status,
                        PaymentStatus paymentStatus) {
                List<OrderItemResponse> items = List.of(
                                new OrderItemResponse(
                                                1L, 1L, "Wireless Mouse",
                                                BigDecimal.valueOf(24.99), 2,
                                                BigDecimal.valueOf(49.98)));
                return new OrderResponse(
                                id, 10L, status, paymentStatus, paymentId,
                                BigDecimal.valueOf(49.98),
                                LocalDateTime.now(), 0L, items);
        }

        private record TestPaymentEvent(
                        String eventId,
                        String eventType,
                        Instant occurredAt,
                        Long paymentId,
                        Long orderId,
                        String amount,
                        String currency) {
        }
}
