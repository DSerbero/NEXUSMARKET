package app.adapters.rest.dtos.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload to add a product to the cart. Only the product reference crosses the
 * boundary: price, status and totals are always resolved server-side.
 */
public record AddCartItemRequest(
        @NotBlank(message = "productId is required")
        String productId) {
}