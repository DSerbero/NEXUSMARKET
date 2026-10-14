package application.adapters.rest.dtos.responses;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        String identifier,
        String type,
        BigDecimal price,
        List<String> variants,
        String status,
        String sellerIdentifier,
        String digitalAsset) {
}