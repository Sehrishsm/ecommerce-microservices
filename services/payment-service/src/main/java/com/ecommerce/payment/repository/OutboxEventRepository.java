package com.ecommerce.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.payment.entity.OutboxEvent;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxEvent.Status status);
}
