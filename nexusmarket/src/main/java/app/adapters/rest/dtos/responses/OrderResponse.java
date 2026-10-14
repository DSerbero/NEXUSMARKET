package app.adapters.rest.dtos.responses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Snapshot of a confirmed order. Items are immutable commercial snapshots:
 * unit price, quantity and currency were captured at checkout time.
 */
public record OrderResponse(
        String identifier,
        String status,
        LocalDateTime creationDate,
        List<OrderItemResponse> items) {
}