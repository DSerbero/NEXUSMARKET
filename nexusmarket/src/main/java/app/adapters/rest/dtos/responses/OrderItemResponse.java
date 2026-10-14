package app.adapters.rest.dtos.responses;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productIdentifier,
        String productType,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String currencyCode) {
}