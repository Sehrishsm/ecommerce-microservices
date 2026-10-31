
package com.ecommerce.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
