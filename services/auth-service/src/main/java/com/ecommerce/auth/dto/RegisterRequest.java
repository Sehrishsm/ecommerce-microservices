
package com.ecommerce.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
                @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 254, message = "Email is too long") String email,

                @NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters") String password,

                @NotBlank(message = "Full name is required") @Size(max = 100, message = "Full name must not exceed 100 characters") String fullName,

                @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber) {
}
