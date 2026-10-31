package com.ecommerce.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecommerce.auth.dto.CreateUserProfileRequest;

@Component
public class UserProfileClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public UserProfileClient(
            RestClient.Builder builder,
            @Value("${app.internal.api-key}") String internalApiKey) {
        this.restClient = builder
                .baseUrl("http://user-service")
                .build();

        this.internalApiKey = internalApiKey;
    }

    public void createProfile(
            Long authUserId,
            String fullName,
            String phoneNumber) {

        CreateUserProfileRequest request = new CreateUserProfileRequest(fullName, phoneNumber);

        restClient.post()
                .uri("/internal/profiles/{authUserId}", authUserId)
                .header("X-Internal-Service-Key", internalApiKey)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}