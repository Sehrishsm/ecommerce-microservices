
package com.ecommerce.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Hello from Payment Service!";
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        Payment payment = paymentService.createPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(
            @PathVariable("orderId") Long orderId) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByOrderId(orderId));
    }

    @PostMapping("/{id}/processing")
    public ResponseEntity<Payment> markPaymentProcessing(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                paymentService.markPaymentProcessing(id));
    }

    @PostMapping("/{id}/success")
    public ResponseEntity<Payment> markPaymentSuccess(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                paymentService.markPaymentSuccess(id));
    }

    @PostMapping("/{id}/failure")
    public ResponseEntity<Payment> markPaymentFailed(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                paymentService.markPaymentFailed(id));
    }

}
