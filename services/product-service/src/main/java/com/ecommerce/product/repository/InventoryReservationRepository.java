package com.ecommerce.product.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ecommerce.product.entity.InventoryReservation;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByOrderIdAndProductId(
            Long orderId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReservation r where r.orderId = :orderId and r.productId = :productId")
    Optional<InventoryReservation> findByOrderIdAndProductIdForUpdate(
            @Param("orderId") Long orderId,
            @Param("productId") Long productId);

    List<InventoryReservation> findAllByOrderId(Long orderId);
}