package app.adapters.rest.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Current cart of the authenticated buyer. An empty item list with a null
 * creation date represents the implicit cart that does not exist yet.
 */
public record CartResponse(
        List<CartItemResponse> items,
        LocalDateTime creationDate) {
}