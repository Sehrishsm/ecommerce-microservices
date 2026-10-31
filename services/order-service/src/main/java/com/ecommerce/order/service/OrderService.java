package com.ecommerce.order.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.client.dto.InventoryReservationRequest.InventoryReservationItem;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxEventService outboxEventService;
    private final InventoryServiceClient inventoryServiceClient;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventService outboxEventService,
            InventoryServiceClient inventoryServiceClient) {
        this.orderRepository = orderRepository;
        this.outboxEventService = outboxEventService;
        this.inventoryServiceClient = inventoryServiceClient;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        Order order = new Order(request.getCustomerId());

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem(
                    itemRequest.getProductId(),
                    itemRequest.getProductName(),
                    itemRequest.getUnitPrice(),
                    itemRequest.getQuantity());

            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);

        List<InventoryReservationItem> reservationItems = request.getItems()
                .stream()
                .map(item -> new InventoryReservationItem(
                        item.getProductId(),
                        item.getQuantity()))
                .toList();

        inventoryServiceClient.reserveStock(
                savedOrder.getId(),
                reservationItems);

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                "OrderCreated",
                Instant.now(),
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(),
                "USD");
        try {
            outboxEventService.saveOrderCreated(event);
        } catch (RuntimeException outboxFailure) {

            // Compensate by releasing the inventory reserved for this order.
            for (Long productId : reservationItems.stream()
                    .map(InventoryReservationItem::productId)
                    .distinct()
                    .toList()) {

                try {
                    inventoryServiceClient.releaseReservation(
                            savedOrder.getId(),
                            productId);

                } catch (RuntimeException compensationFailure) {
                    outboxFailure.addSuppressed(compensationFailure);
                }
            }

            throw outboxFailure;
        }

        return OrderResponse.from(savedOrder);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = findOrder(orderId);

        return OrderResponse.from(order);
    }

    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException(
                    "Customer ID must be a positive number");
        }

        return orderRepository
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse beginPayment(Long orderId) {
        Order order = findOrder(orderId);
        order.beginPayment();

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse associatePayment(Long orderId, Long paymentId) {
        Order order = findOrder(orderId);
        order.associatePayment(paymentId);

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse recordPaymentSuccess(Long orderId) {
        Order order = findOrder(orderId);
        order.recordPaymentSuccess();

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse recordPaymentFailure(Long orderId) {
        Order order = findOrder(orderId);
        order.recordPaymentFailure();

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = findOrder(orderId);
        order.cancel();

        // Diagnostic logging: verify inventory compensation during cancellation.
        logger.info("Cancellation started: orderId={}, itemCount={}",
                order.getId(), order.getItems().size());

        for (var item : order.getItems()) {
            logger.info("Releasing inventory: orderId={}, productId={}, quantity={}",
                    order.getId(), item.getProductId(), item.getQuantity());

            try {
                var reservation = inventoryServiceClient.releaseReservation(
                        order.getId(),
                        item.getProductId());

                logger.info("Inventory release response: orderId={}, productId={}, status={}",
                        order.getId(),
                        item.getProductId(),
                        reservation == null ? "null" : reservation.status());

            } catch (RuntimeException failure) {
                logger.error("Inventory release failed: orderId={}, productId={}",
                        order.getId(), item.getProductId(), failure);
                throw failure;
            }
        }

        logger.info("Inventory release loop completed: orderId={}", order.getId());

        OrderCancelledEvent event = new OrderCancelledEvent(
                UUID.randomUUID().toString(),
                "OrderCancelled",
                Instant.now(),
                order.getId(),
                order.getCustomerId());

        outboxEventService.saveOrderCancelled(event);

        return OrderResponse.from(order);
    }

    private Order findOrder(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException(
                    "Order ID must be a positive number");
        }

        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
