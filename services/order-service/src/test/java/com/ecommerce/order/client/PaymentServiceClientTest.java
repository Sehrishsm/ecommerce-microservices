
package com.ecommerce.order.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ecommerce.order.client.dto.PaymentServiceResponse;

class PaymentServiceClientTest {

    @Test
    void createPaymentShouldSendRequestAndReadResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        PaymentServiceClient client = new PaymentServiceClient(builder);

        server.expect(requestTo("http://payment-service/payments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "orderId": 5,
                          "amount": 51.00,
                          "idempotencyKey": "order-5-payment"
                        }
                        """))

                .andRespond(
                        org.springframework.test.web.client.response.MockRestResponseCreators
                                .withSuccess(
                                        """
                                                {
                                                  "id": 12,
                                                  "idempotencyKey": "order-5-payment",
                                                  "orderId": 5,
                                                  "amount": 51.00,
                                                  "status": "PENDING",
                                                  "createdAt": "2026-10-23T12:00:00",
                                                  "updatedAt": "2026-10-23T12:00:00",
                                                  "version": 0
                                                }
                                                """,
                                        MediaType.APPLICATION_JSON));

        PaymentServiceClient.PaymentRequestPayload request = new PaymentServiceClient.PaymentRequestPayload(
                5L,
                new BigDecimal("51.00"),
                "order-5-payment");

        PaymentServiceResponse response = client.createPayment(request);

        assertEquals(12L, response.id());
        assertEquals(5L, response.orderId());
        assertEquals(new BigDecimal("51.00"), response.amount());
        assertEquals("PENDING", response.status());

        server.verify();
    }
}
