
package com.ecommerce.user.dto;

import java.time.Instant;

public record UserProfileResponse(
        Long authUserId,
        String fullName,
        String phoneNumber,
        Instant createdAt,
        Instant updatedAt) {
}
