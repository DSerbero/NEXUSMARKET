package app.adapters.rest.dtos.responses;

import java.math.BigDecimal;

public record CartItemResponse(
        String productIdentifier,
        int quantity,
        BigDecimal unitPrice) {
}