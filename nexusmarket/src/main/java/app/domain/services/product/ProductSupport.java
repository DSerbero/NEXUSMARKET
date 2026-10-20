package app.domain.services.product;

import java.math.BigDecimal;
import java.util.ArrayList;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
import app.domain.models.Product;

/**
 * Shared catalog invariant: a product must carry a positive price and, when
 * digital, a non-blank asset reference. Variants are normalized to an empty
 * list. Used by registration and update (SRP: the rule lives in one place).
 */
final class ProductSupport {

    private ProductSupport() {
    }

    static void validateProduct(Product product) {
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("price must be greater than zero");
        }
        if (product.getVariants() == null) {
            product.setVariants(new ArrayList<>());
        }
        if (product instanceof DigitalProduct digitalProduct
                && (digitalProduct.getDigitalAsset() == null || digitalProduct.getDigitalAsset().isBlank())) {
            throw new DomainValidationException("digitalAsset is required");
        }
    }
}