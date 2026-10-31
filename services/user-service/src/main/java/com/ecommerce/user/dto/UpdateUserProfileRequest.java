
package com.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(

        @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name must not exceed 100 characters") String fullName,

        @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber

) {
}
