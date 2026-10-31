
package com.ecommerce.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType) {
}
