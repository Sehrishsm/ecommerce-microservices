
package com.ecommerce.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

        @Mock
        private PaymentRepository paymentRepository;

        private PaymentService paymentService;
        @Mock
        private PaymentOutboxEventService paymentOutboxEventService;

        @BeforeEach
        void setUp() {
                paymentService = new PaymentService(
                                paymentRepository, paymentOutboxEventService);
        }

        @Test
        void createPaymentShouldSaveNewPayment() {
                PaymentRequest request = createRequest(
                                10L, "49.90", "payment-key-1");

                when(paymentRepository.findByIdempotencyKey("payment-key-1"))
                                .thenReturn(Optional.empty());

                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Payment result = paymentService.createPayment(request);

                assertEquals(10L, result.getOrderId());
                assertEquals(0, new BigDecimal("49.90")
                                .compareTo(result.getAmount()));
                assertEquals("payment-key-1", result.getIdempotencyKey());
                assertEquals(PaymentStatus.PENDING, result.getStatus());

                verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        void createPaymentShouldReturnExistingPaymentForSameRequest() {
                PaymentRequest request = createRequest(
                                10L, "49.90", "payment-key-1");

                Payment existing = new Payment(
                                10L, new BigDecimal("49.90"), "payment-key-1");

                when(paymentRepository.findByIdempotencyKey("payment-key-1"))
                                .thenReturn(Optional.of(existing));

                Payment result = paymentService.createPayment(request);

                assertSame(existing, result);
                verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        void createPaymentShouldRejectSameKeyWithDifferentDetails() {
                PaymentRequest request = createRequest(
                                10L, "60.00", "payment-key-1");

                Payment existing = new Payment(
                                10L, new BigDecimal("49.90"), "payment-key-1");

                when(paymentRepository.findByIdempotencyKey("payment-key-1"))
                                .thenReturn(Optional.of(existing));

                assertThrows(
                                IllegalStateException.class,
                                () -> paymentService.createPayment(request));

                verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        void getPaymentByIdShouldRejectMissingPayment() {
                when(paymentRepository.findById(999L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> paymentService.getPaymentById(999L));
        }

        @Test
        void markPaymentProcessingShouldSaveProcessingPayment() {
                Payment payment = new Payment(
                                10L, new BigDecimal("49.90"), "payment-key-2");

                when(paymentRepository.findById(1L))
                                .thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Payment result = paymentService.markPaymentProcessing(1L);

                assertEquals(PaymentStatus.PROCESSING, result.getStatus());
                verify(paymentRepository).save(payment);
        }

        @Test
        void markPaymentSuccessShouldSaveSuccessfulPayment() {
                Payment payment = new Payment(
                                10L, new BigDecimal("49.90"), "payment-key-3");
                payment.markProcessing();

                when(paymentRepository.findById(2L))
                                .thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Payment result = paymentService.markPaymentSuccess(2L);

                assertEquals(PaymentStatus.SUCCESS, result.getStatus());
                verify(paymentRepository).save(payment);
                verify(paymentOutboxEventService).savePaymentSucceeded(payment);
        }

        @Test
        void markPaymentFailedShouldSaveFailedPayment() {
                Payment payment = new Payment(
                                10L, new BigDecimal("49.90"), "payment-key-4");

                when(paymentRepository.findById(3L))
                                .thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                Payment result = paymentService.markPaymentFailed(3L);
                verify(paymentOutboxEventService).savePaymentFailed(payment);
                assertEquals(PaymentStatus.FAILED, result.getStatus());
                verify(paymentRepository).save(payment);
        }

        @Test
        void markPaymentSuccessShouldRejectPendingPayment() {
                Payment payment = new Payment(
                                10L, new BigDecimal("49.90"), "payment-key-5");

                when(paymentRepository.findById(4L))
                                .thenReturn(Optional.of(payment));

                assertThrows(
                                IllegalStateException.class,
                                () -> paymentService.markPaymentSuccess(4L));

                verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        void markPaymentProcessingShouldRejectMissingPayment() {
                when(paymentRepository.findById(999L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> paymentService.markPaymentProcessing(999L));

                verify(paymentRepository, never()).save(any(Payment.class));
        }

        private PaymentRequest createRequest(
                        Long orderId, String amount, String idempotencyKey) {

                PaymentRequest request = new PaymentRequest();
                request.setOrderId(orderId);
                request.setAmount(new BigDecimal(amount));
                request.setIdempotencyKey(idempotencyKey);
                return request;
        }
}
