package app.adapters.rest.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Transport contract for the login endpoint. Credentials are primitives by
 * necessity (no authenticated user exists yet), matching the SDD. Bean
 * validation rejects blank or malformed input before it reaches the domain.
 */
public record LoginRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,
        @NotBlank(message = "password is required")
        String password) {
}
