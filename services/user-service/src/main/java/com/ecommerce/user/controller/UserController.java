package com.ecommerce.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import com.ecommerce.user.dto.UpdateUserProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.service.UserProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {

    private final RestClient restClient;
    private final UserProfileService profileService;

    public UserController(RestClient.Builder builder, UserProfileService profileService) {
        this.restClient = builder.build();
        this.profileService = profileService;
    }

    @GetMapping("/product-check")
    public String checkProductService() {
        return restClient.get()
                .uri("http://product-service/products/ping")
                .retrieve()
                .body(String.class);
    }

    // Existing service-discovery demonstration is preserved.

    // @PostMapping("/{authUserId}/profile")
    // @ResponseStatus(HttpStatus.CREATED)
    // public UserProfileResponse createProfile(
    // @PathVariable Long authUserId,
    // @Valid @RequestBody CreateUserProfileRequest request) {
    // return profileService.create(authUserId, request);
    // }

    @GetMapping("/{authUserId}/profile")
    public UserProfileResponse getProfile(
            @PathVariable("authUserId") Long authUserId) {
        return profileService.getByAuthUserId(authUserId);
    }

    @PutMapping("/{authUserId}/profile")
    public UserProfileResponse updateProfile(
            @PathVariable("authUserId") Long authUserId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return profileService.update(authUserId, request);
    }
}
