package com.ecommerce.order.dto;

import com.ecommerce.order.client.dto.PaymentServiceResponse;

public record PaymentInitiationResponse(
        OrderResponse order,
        PaymentServiceResponse payment) {
}
