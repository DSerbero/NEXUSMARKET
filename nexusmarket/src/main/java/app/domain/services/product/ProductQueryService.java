package app.domain.services.product;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.EntityNotFoundException;
import app.domain.models.Product;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.DomainValidations;
import lombok.RequiredArgsConstructor;

/**
 * Single authority for loading the current state of a product. Every catalog
 * or lifecycle operation starts here, so the "read before write" step is never
 * duplicated nor skipped (SRP).
 */
@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepositoryPort productRepository;

    public Product authoritative(Product product) {
        DomainValidations.required(product, "product");
        return productRepository.findByIdentifier(product)
                .orElseThrow(() -> new EntityNotFoundException("Product"));
    }
}