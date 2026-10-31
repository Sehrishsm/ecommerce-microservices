
package com.ecommerce.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.order.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
