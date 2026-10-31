
package com.ecommerce.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentInitiationResponse;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.PaymentInitiationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentInitiationService paymentInitiationService;

    public OrderController(
            OrderService orderService,
            PaymentInitiationService paymentInitiationService) {
        this.orderService = orderService;
        this.paymentInitiationService = paymentInitiationService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "Hello from Order Service!";
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(
            @PathVariable("customerId") Long customerId) {

        return ResponseEntity.ok(
                orderService.getOrdersByCustomerId(customerId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<PaymentInitiationResponse> initiatePayment(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(
                paymentInitiationService.initiatePayment(id));
    }
}
