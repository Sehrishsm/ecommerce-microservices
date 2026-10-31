package com.ecommerce.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);
}