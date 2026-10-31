
package com.ecommerce.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.client.dto.InventoryReservationRequest.InventoryReservationItem;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;
import com.ecommerce.order.event.OrderCancelledEvent;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

        @Mock
        private OrderRepository orderRepository;
        @Mock
        private OutboxEventService outboxEventService;
        private OrderService orderService;
        @Mock
        private InventoryServiceClient inventoryServiceClient;

        @BeforeEach
        void setUp() {
                orderService = new OrderService(
                                orderRepository,
                                outboxEventService,
                                inventoryServiceClient);
        }

        @Test
        void createOrderShouldSaveOrderWithCalculatedTotal() {
                OrderCreateRequest request = new OrderCreateRequest();
                request.setCustomerId(10L);

                OrderItemRequest item = new OrderItemRequest();
                item.setProductId(1L);
                item.setProductName("Keyboard");
                item.setUnitPrice(new BigDecimal("25.50"));
                item.setQuantity(2);

                request.setItems(List.of(item));

                when(orderRepository.save(any(Order.class)))
                                .thenAnswer(invocation -> {
                                        Order savedOrder = invocation.getArgument(0);

                                        try {
                                                var idField = Order.class.getDeclaredField("id");
                                                idField.setAccessible(true);
                                                idField.set(savedOrder, 101L);
                                        } catch (ReflectiveOperationException exception) {
                                                throw new IllegalStateException(exception);
                                        }

                                        return savedOrder;
                                });

                // Execute the operation before verifying its interactions.
                OrderResponse response = orderService.createOrder(request);

                assertEquals(10L, response.getCustomerId());
                assertEquals(new BigDecimal("51.00"), response.getTotalAmount());
                assertEquals(OrderStatus.PENDING, response.getStatus());
                assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
                assertEquals(1, response.getItems().size());
                ArgumentCaptor<List<InventoryReservationItem>> reservationCaptor = ArgumentCaptor.forClass(List.class);

                verify(inventoryServiceClient).reserveStock(
                                org.mockito.ArgumentMatchers.eq(101L),
                                reservationCaptor.capture());

                assertEquals(1, reservationCaptor.getValue().size());
                assertEquals(1L, reservationCaptor.getValue().get(0).productId());
                assertEquals(2, reservationCaptor.getValue().get(0).quantity());
                verify(orderRepository).save(any(Order.class));

                ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);

                verify(outboxEventService).saveOrderCreated(eventCaptor.capture());

                OrderCreatedEvent publishedEvent = eventCaptor.getValue();

                assertEquals("OrderCreated", publishedEvent.eventType());
                assertEquals(101L, publishedEvent.orderId());
                assertEquals(10L, publishedEvent.customerId());
                assertEquals(new BigDecimal("51.00"), publishedEvent.totalAmount());
                assertEquals("USD", publishedEvent.currency());
        }

        @Test
        void createOrderShouldNotPublishEventWhenInventoryReservationFails() {
                OrderCreateRequest request = new OrderCreateRequest();
                request.setCustomerId(10L);

                OrderItemRequest item = new OrderItemRequest();
                item.setProductId(1L);
                item.setProductName("Keyboard");
                item.setUnitPrice(new BigDecimal("25.50"));
                item.setQuantity(2);
                request.setItems(List.of(item));

                when(orderRepository.save(any(Order.class)))
                                .thenAnswer(invocation -> {
                                        Order savedOrder = invocation.getArgument(0);

                                        try {
                                                var idField = Order.class.getDeclaredField("id");
                                                idField.setAccessible(true);
                                                idField.set(savedOrder, 101L);
                                        } catch (ReflectiveOperationException exception) {
                                                throw new IllegalStateException(exception);
                                        }

                                        return savedOrder;
                                });

                RuntimeException reservationFailure = new IllegalStateException("Inventory reservation failed");

                doThrow(reservationFailure)
                                .when(inventoryServiceClient)
                                .reserveStock(
                                                org.mockito.ArgumentMatchers.eq(101L),
                                                any());

                RuntimeException thrown = assertThrows(
                                RuntimeException.class,
                                () -> orderService.createOrder(request));

                assertEquals(reservationFailure, thrown);
                verify(orderRepository).save(any(Order.class));
                verifyNoInteractions(outboxEventService);
        }

        @Test
        void createOrderShouldReleaseInventoryWhenOutboxSavingFails() {
                OrderCreateRequest request = new OrderCreateRequest();
                request.setCustomerId(10L);

                OrderItemRequest firstItem = new OrderItemRequest();
                firstItem.setProductId(1L);
                firstItem.setProductName("Keyboard");
                firstItem.setUnitPrice(new BigDecimal("25.00"));
                firstItem.setQuantity(2);

                OrderItemRequest secondItem = new OrderItemRequest();
                secondItem.setProductId(2L);
                secondItem.setProductName("Mouse");
                secondItem.setUnitPrice(new BigDecimal("15.00"));
                secondItem.setQuantity(1);

                OrderItemRequest duplicateItem = new OrderItemRequest();
                duplicateItem.setProductId(1L);
                duplicateItem.setProductName("Keyboard");
                duplicateItem.setUnitPrice(new BigDecimal("25.00"));
                duplicateItem.setQuantity(1);

                request.setItems(List.of(firstItem, secondItem, duplicateItem));

                when(orderRepository.save(any(Order.class)))
                                .thenAnswer(invocation -> {
                                        Order savedOrder = invocation.getArgument(0);

                                        try {
                                                var idField = Order.class.getDeclaredField("id");
                                                idField.setAccessible(true);
                                                idField.set(savedOrder, 101L);
                                        } catch (ReflectiveOperationException exception) {
                                                throw new IllegalStateException(exception);
                                        }

                                        return savedOrder;
                                });

                RuntimeException outboxFailure = new IllegalStateException("Outbox saving failed");

                doThrow(outboxFailure)
                                .when(outboxEventService)
                                .saveOrderCreated(any(OrderCreatedEvent.class));

                RuntimeException thrown = assertThrows(
                                RuntimeException.class,
                                () -> orderService.createOrder(request));

                assertEquals(outboxFailure, thrown);

                verify(inventoryServiceClient).releaseReservation(101L, 1L);
                verify(inventoryServiceClient).releaseReservation(101L, 2L);

                verify(inventoryServiceClient).reserveStock(
                                org.mockito.ArgumentMatchers.eq(101L),
                                any());
        }

        @Test
        void getOrderByIdShouldReturnExistingOrder() {
                Order order = new Order(10L);

                order.addItem(new OrderItem(
                                1L, "Keyboard", new BigDecimal("25.00"), 1));

                when(orderRepository.findById(5L))
                                .thenReturn(Optional.of(order));

                OrderResponse response = orderService.getOrderById(5L);

                assertEquals(10L, response.getCustomerId());
                assertEquals(new BigDecimal("25.00"), response.getTotalAmount());

                verify(orderRepository).findById(5L);
        }

        @Test
        void getOrderByIdShouldThrowWhenOrderDoesNotExist() {
                when(orderRepository.findById(99L))
                                .thenReturn(Optional.empty());

                OrderNotFoundException exception = assertThrows(
                                OrderNotFoundException.class,
                                () -> orderService.getOrderById(99L));

                assertEquals("Order not found: 99", exception.getMessage());
        }

        @Test
        void getOrdersByCustomerIdShouldReturnCustomerOrders() {
                Order firstOrder = new Order(10L);
                Order secondOrder = new Order(10L);

                when(orderRepository.findByCustomerIdOrderByCreatedAtDesc(10L))
                                .thenReturn(List.of(firstOrder, secondOrder));

                List<OrderResponse> responses = orderService.getOrdersByCustomerId(10L);

                assertEquals(2, responses.size());
                assertEquals(10L, responses.get(0).getCustomerId());
                assertEquals(10L, responses.get(1).getCustomerId());

                verify(orderRepository)
                                .findByCustomerIdOrderByCreatedAtDesc(10L);
        }

        @Test
        void getOrdersByCustomerIdShouldRejectInvalidId() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.getOrdersByCustomerId(0L));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.getOrdersByCustomerId(-1L));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.getOrdersByCustomerId(null));

                verifyNoInteractions(orderRepository);
        }

        @Test
        void getOrderByIdShouldRejectInvalidId() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.getOrderById(0L));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.getOrderById(-1L));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> orderService.getOrderById(null));

                verifyNoInteractions(orderRepository);
        }

        @Test
        void beginPaymentShouldMoveOrderToPaymentPending() {
                Order order = new Order(10L);

                when(orderRepository.findById(5L))
                                .thenReturn(Optional.of(order));

                OrderResponse response = orderService.beginPayment(5L);

                assertEquals(OrderStatus.PAYMENT_PENDING, response.getStatus());
                assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        }

        @Test
        void recordPaymentSuccessShouldConfirmOrder() {
                Order order = new Order(10L);
                order.beginPayment();

                when(orderRepository.findById(5L))
                                .thenReturn(Optional.of(order));

                OrderResponse response = orderService.recordPaymentSuccess(5L);

                assertEquals(OrderStatus.CONFIRMED, response.getStatus());
                assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        }

        @Test
        void recordPaymentFailureShouldMarkOrderAsPaymentFailed() {
                Order order = new Order(10L);
                order.beginPayment();

                when(orderRepository.findById(5L))
                                .thenReturn(Optional.of(order));

                OrderResponse response = orderService.recordPaymentFailure(5L);

                assertEquals(OrderStatus.PAYMENT_FAILED, response.getStatus());
                assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
        }

        @Test
        void cancelOrderShouldCancelPendingOrderAndPublishEvent() {
                Order order = new Order(10L);
                order.addItem(new OrderItem(
                                101L,
                                "Wireless Mouse",
                                new BigDecimal("24.99"),
                                2));

                try {
                        var idField = Order.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(order, 5L);
                } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException(exception);
                }

                when(orderRepository.findById(5L))
                                .thenReturn(Optional.of(order));

                OrderResponse response = orderService.cancelOrder(5L);

                assertEquals(OrderStatus.CANCELLED, response.getStatus());
                verify(inventoryServiceClient).releaseReservation(5L, 101L);

                ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
                verify(outboxEventService).saveOrderCancelled(eventCaptor.capture());

                OrderCancelledEvent publishedEvent = eventCaptor.getValue();
                assertEquals("OrderCancelled", publishedEvent.eventType());
                assertEquals(5L, publishedEvent.orderId());
                assertEquals(10L, publishedEvent.customerId());
        }

        @Test
        void cancelOrderShouldNotPublishEventWhenOrderIsConfirmed() {
                Order order = new Order(10L);
                order.beginPayment();
                order.recordPaymentSuccess();

                when(orderRepository.findById(5L))
                                .thenReturn(Optional.of(order));

                assertThrows(
                                IllegalStateException.class,
                                () -> orderService.cancelOrder(5L));

                verifyNoInteractions(outboxEventService, inventoryServiceClient);
        }

        @Test
        void paymentOperationShouldThrowWhenOrderDoesNotExist() {
                when(orderRepository.findById(99L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                OrderNotFoundException.class,
                                () -> orderService.beginPayment(99L));

                assertThrows(
                                OrderNotFoundException.class,
                                () -> orderService.recordPaymentSuccess(99L));

                assertThrows(
                                OrderNotFoundException.class,
                                () -> orderService.recordPaymentFailure(99L));

                assertThrows(
                                OrderNotFoundException.class,
                                () -> orderService.cancelOrder(99L));
        }
}
