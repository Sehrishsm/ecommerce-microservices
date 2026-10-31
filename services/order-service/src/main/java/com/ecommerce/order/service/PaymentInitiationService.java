package com.ecommerce.order.service;

import org.springframework.stereotype.Service;

import com.ecommerce.order.client.PaymentServiceClient;
import com.ecommerce.order.client.PaymentServiceClient.PaymentRequestPayload;
import com.ecommerce.order.client.dto.PaymentServiceResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentInitiationResponse;
import com.ecommerce.order.entity.enums.OrderStatus;

@Service
public class PaymentInitiationService {

    private final OrderService orderService;
    private final PaymentServiceClient paymentServiceClient;

    public PaymentInitiationService(
            OrderService orderService,
            PaymentServiceClient paymentServiceClient) {
        this.orderService = orderService;
        this.paymentServiceClient = paymentServiceClient;
    }

    public PaymentInitiationResponse initiatePayment(Long orderId) {
        OrderResponse order = orderService.getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PENDING) {
            order = orderService.beginPayment(orderId);
        } else if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException(
                    "Payment can only be initiated for a pending order");
        }

        String idempotencyKey = "order-" + order.getId() + "-payment";

        PaymentRequestPayload request = new PaymentRequestPayload(
                order.getId(),
                order.getTotalAmount(),
                idempotencyKey);

        PaymentServiceResponse payment = paymentServiceClient.createPayment(request);

        // Save the Payment Service ID against this order.
        order = orderService.associatePayment(orderId, payment.id());

        return new PaymentInitiationResponse(order, payment);
    }
}
