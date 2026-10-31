
package com.ecommerce.payment.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.repository.PaymentRepository;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxEventService paymentOutboxEventService;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentOutboxEventService paymentOutboxEventService) {
        this.paymentRepository = paymentRepository;
        this.paymentOutboxEventService = paymentOutboxEventService;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + id));
    }

    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional
    public Payment createPayment(PaymentRequest request) {
        String idempotencyKey = request.getIdempotencyKey().trim();

        Payment existingPayment = paymentRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingPayment != null) {
            boolean sameOrder = existingPayment.getOrderId()
                    .equals(request.getOrderId());

            boolean sameAmount = existingPayment.getAmount()
                    .compareTo(request.getAmount()) == 0;

            if (!sameOrder || !sameAmount) {
                throw new IllegalStateException(
                        "Idempotency key has already been used "
                                + "with different payment details");
            }

            return existingPayment;
        }

        Payment payment = new Payment(
                request.getOrderId(),
                request.getAmount(),
                idempotencyKey);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markPaymentSuccess(Long id) {
        Payment payment = getPaymentById(id);
        payment.markSuccess();

        Payment savedPayment = paymentRepository.save(payment);
        paymentOutboxEventService.savePaymentSucceeded(savedPayment);

        return savedPayment;
    }

    @Transactional
    public Payment markPaymentFailed(Long id) {
        Payment payment = getPaymentById(id);
        payment.markFailed();

        Payment savedPayment = paymentRepository.save(payment);
        paymentOutboxEventService.savePaymentFailed(savedPayment);

        return savedPayment;
    }

    // @Transactional
    // public Payment markPaymentFailed(Long id) {
    // Payment payment = getPaymentById(id);
    // payment.markFailed();
    // return paymentRepository.save(payment);
    // }

    @Transactional
    public Payment markPaymentProcessing(Long id) {
        Payment payment = getPaymentById(id);
        payment.markProcessing();
        return paymentRepository.save(payment);
    }
}
