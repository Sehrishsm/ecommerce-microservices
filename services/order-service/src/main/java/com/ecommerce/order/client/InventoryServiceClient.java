package com.ecommerce.order.client;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.order.client.dto.InventoryReservationRequest;
import com.ecommerce.order.client.dto.InventoryReservationRequest.InventoryReservationItem;
import com.ecommerce.order.client.dto.InventoryReservationResponse;

@Component
public class InventoryServiceClient {

    private final RestClient restClient;

    public InventoryServiceClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("http://product-service")
                .build();
    }

    public List<InventoryReservationResponse> reserveStock(
            Long orderId,
            List<InventoryReservationItem> items) {

        InventoryReservationRequest request =
                new InventoryReservationRequest(orderId, items);

        InventoryReservationResponse[] response = restClient.post()
                .uri("/inventory/reservations")
                .body(request)
                .retrieve()
                .body(InventoryReservationResponse[].class);

        if (response == null) {
            throw new IllegalStateException(
                    "Product Service returned an empty reservation response");
        }

        return List.of(response);
    }

    public InventoryReservationResponse releaseReservation(
            Long orderId,
            Long productId) {

        return restClient.post()
                .uri("/inventory/reservations/{orderId}/{productId}/release",
                        orderId, productId)
                .retrieve()
                .body(InventoryReservationResponse.class);
    }

    public InventoryReservationResponse commitReservation(
            Long orderId,
            Long productId) {

        return restClient.post()
                .uri("/inventory/reservations/{orderId}/{productId}/commit",
                        orderId, productId)
                .retrieve()
                .body(InventoryReservationResponse.class);
    }
}
