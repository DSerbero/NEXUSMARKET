package app.domain.services.product;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Product;
import app.domain.valueobjects.ProductStatus;
import lombok.RequiredArgsConstructor;

/**
 * Consult Published Product: public read that only returns PUBLISHED products,
 * reported as "not found" otherwise. Used by the public catalog endpoint.
 */
@Service
@ConditionalOnBean(ProductQueryService.class)
@RequiredArgsConstructor
public class ConsultPublishedProductService {

    private final ProductQueryService productQuery;

    public Product execute(Product product) {
        Product current = productQuery.authoritative(product);
        if (!ProductStatus.PUBLISHED.equals(current.getStatus())) {
            throw new DomainValidationException("Product not found");
        }
        return current;
    }
}