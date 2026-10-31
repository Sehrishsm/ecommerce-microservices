package com.ecommerce.user.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerce.user.dto.CreateUserProfileRequest;
import com.ecommerce.user.dto.UserProfileResponse;
import com.ecommerce.user.service.UserProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/profiles")
public class InternalProfileController {

    private final UserProfileService profileService;
    private final String internalApiKey;

    public InternalProfileController(
            UserProfileService profileService,
            @Value("${app.internal.api-key}") String internalApiKey) {
        this.profileService = profileService;
        this.internalApiKey = internalApiKey;
    }

    @PostMapping("/{authUserId}")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse createProfile(
            @PathVariable("authUserId") Long authUserId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false) String suppliedKey,
            @Valid @RequestBody CreateUserProfileRequest request) {

        if (suppliedKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Service authentication failed");
        }

        return profileService.create(authUserId, request);
    }
}