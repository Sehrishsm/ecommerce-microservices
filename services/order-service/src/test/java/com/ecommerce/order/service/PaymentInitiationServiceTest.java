package com.ecommerce.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import com.ecommerce.order.client.PaymentServiceClient;
import com.ecommerce.order.client.PaymentServiceClient.PaymentRequestPayload;
import com.ecommerce.order.client.dto.PaymentServiceResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentInitiationResponse;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;

@ExtendWith(MockitoExtension.class)
class PaymentInitiationServiceTest {

        @Mock
        private OrderService orderService;

        @Mock
        private PaymentServiceClient paymentServiceClient;

        private PaymentInitiationService paymentInitiationService;

        @BeforeEach
        void setUp() {
                paymentInitiationService = new PaymentInitiationService(
                                orderService,
                                paymentServiceClient);
        }

        @Test
        void initiatePaymentShouldBeginPaymentAndCreatePaymentRequest() {
                when(orderService.getOrderById(5L))
                                .thenReturn(orderResponse(OrderStatus.PENDING));

                when(orderService.beginPayment(5L))
                                .thenReturn(orderResponse(OrderStatus.PAYMENT_PENDING));
                when(orderService.associatePayment(5L, 12L))
                                .thenReturn(orderResponse(OrderStatus.PAYMENT_PENDING, 12L));

                when(paymentServiceClient.createPayment(any(PaymentRequestPayload.class)))
                                .thenReturn(paymentResponse());

                PaymentInitiationResponse response = paymentInitiationService.initiatePayment(5L);

                assertEquals(OrderStatus.PAYMENT_PENDING, response.order().getStatus());
                assertEquals("PENDING", response.payment().status());
                assertEquals(12L, response.order().getPaymentId());
                assertEquals(12L, response.payment().id());

                ArgumentCaptor<PaymentRequestPayload> captor = ArgumentCaptor.forClass(PaymentRequestPayload.class);

                verify(paymentServiceClient).createPayment(captor.capture());

                PaymentRequestPayload request = captor.getValue();

                assertEquals(5L, request.orderId());
                assertEquals(new BigDecimal("51.00"), request.amount());
                assertEquals("order-5-payment", request.idempotencyKey());

                verify(orderService).beginPayment(5L);
                verify(orderService).associatePayment(5L, 12L);
        }

        @Test
        void initiatePaymentShouldReuseExistingPaymentPendingOrder() {
                when(orderService.getOrderById(5L))
                                .thenReturn(orderResponse(OrderStatus.PAYMENT_PENDING));

                when(paymentServiceClient.createPayment(any(PaymentRequestPayload.class)))
                                .thenReturn(paymentResponse());
                when(orderService.associatePayment(5L, 12L))
                                .thenReturn(orderResponse(OrderStatus.PAYMENT_PENDING, 12L));

                PaymentInitiationResponse response = paymentInitiationService.initiatePayment(5L);

                assertEquals(OrderStatus.PAYMENT_PENDING, response.order().getStatus());

                ArgumentCaptor<PaymentRequestPayload> captor = ArgumentCaptor.forClass(PaymentRequestPayload.class);

                verify(paymentServiceClient).createPayment(captor.capture());

                assertEquals("order-5-payment", captor.getValue().idempotencyKey());
                assertEquals(12L, response.order().getPaymentId());
                assertEquals(12L, response.payment().id());

                verify(orderService, never()).beginPayment(5L);
                verify(orderService).associatePayment(5L, 12L);
        }

        @Test
        void initiatePaymentShouldRejectOrderInInvalidState() {
                when(orderService.getOrderById(5L))
                                .thenReturn(orderResponse(OrderStatus.CONFIRMED));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> paymentInitiationService.initiatePayment(5L));

                assertEquals(
                                "Payment can only be initiated for a pending order",
                                exception.getMessage());

                verify(orderService, never()).beginPayment(5L);
                verify(paymentServiceClient, never()).createPayment(any());
        }

        @Test
        void initiatePaymentShouldPropagatePaymentServiceFailure() {
                when(orderService.getOrderById(5L))
                                .thenReturn(orderResponse(OrderStatus.PENDING));

                when(orderService.beginPayment(5L))
                                .thenReturn(orderResponse(OrderStatus.PAYMENT_PENDING));

                when(paymentServiceClient.createPayment(any(PaymentRequestPayload.class)))
                                .thenThrow(new RestClientException("Payment service unavailable"));

                RestClientException exception = assertThrows(
                                RestClientException.class,
                                () -> paymentInitiationService.initiatePayment(5L));

                assertEquals("Payment service unavailable", exception.getMessage());

                verify(orderService).beginPayment(5L);
                verify(orderService, never()).recordPaymentFailure(5L);
                verify(orderService, never()).associatePayment(5L, 12L);
        }

        private OrderResponse orderResponse(OrderStatus status) {
                return new OrderResponse(
                                5L,
                                10L,
                                status,
                                PaymentStatus.PENDING,
                                new BigDecimal("51.00"),
                                LocalDateTime.of(2026, 10, 23, 12, 0),
                                0L,
                                List.of());
        }

        private OrderResponse orderResponse(OrderStatus status, Long paymentId) {
                return new OrderResponse(
                                5L,
                                10L,
                                status,
                                PaymentStatus.PENDING,
                                paymentId,
                                new BigDecimal("51.00"),
                                LocalDateTime.of(2026, 10, 23, 12, 0),
                                0L,
                                List.of());
        }

        private PaymentServiceResponse paymentResponse() {
                LocalDateTime timestamp = LocalDateTime.of(2026, 10, 23, 12, 0);

                return new PaymentServiceResponse(
                                12L,
                                "order-5-payment",
                                5L,

                                new BigDecimal("51.00"),
                                "PENDING",
                                timestamp,
                                timestamp,
                                0L);
        }
}
