package com.ecommerce.auth.dto;

public record CreateUserProfileRequest(
        String fullName,
        String phoneNumber) {
}