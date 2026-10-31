
package com.ecommerce.order.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.order.client.dto.PaymentServiceResponse;

@Component
public class PaymentServiceClient {

    private final RestClient restClient;

    public PaymentServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://payment-service")
                .build();
    }

    public PaymentServiceResponse createPayment(
            PaymentRequestPayload request) {

        return restClient.post()
                .uri("/payments")
                .body(request)
                .retrieve()
                .body(PaymentServiceResponse.class);
    }

    public record PaymentRequestPayload(
            Long orderId,
            java.math.BigDecimal amount,
            String idempotencyKey) {
    }
}
