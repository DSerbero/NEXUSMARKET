package app.domain.services.product;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateOwnershipService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Product status state machine: publish, suspend and discontinue. Catalog data
 * (price, variants) is handled by {@link ProductCatalogService} (SRP).
 */
@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class ProductLifecycleService {

    private final ProductRepositoryPort productRepository;
    private final ProductQueryService productQuery;
    private final ValidateActiveUserService validateActiveUser;
    private final ValidateRoleService validateRole;
    private final ValidateOwnershipService validateOwnership;

    public Product publish(Seller seller, Product product) {
        return updateStatus(seller, product, ProductStatus.SUSPENDED, ProductStatus.PUBLISHED);
    }

    public Product suspend(Seller seller, Product product) {
        return updateStatus(seller, product, ProductStatus.PUBLISHED, ProductStatus.SUSPENDED);
    }

    public Product discontinue(Seller seller, Product product) {
        validateActiveUser.execute(seller);
        validateRole.execute(seller, UserRole.SELLER);
        Product current = productQuery.authoritative(product);
        validateOwnership.execute(seller, current.getSeller());
        if (!ProductStatus.PUBLISHED.equals(current.getStatus())
                && !ProductStatus.SUSPENDED.equals(current.getStatus())) {
            throw new DomainValidationException("Product cannot be discontinued from its current status");
        }
        current.setStatus(ProductStatus.DISCONTINUED);
        return productRepository.update(current);
    }

    private Product updateStatus(Seller seller, Product product, ProductStatus expected, ProductStatus next) {
        validateActiveUser.execute(seller);
        validateRole.execute(seller, UserRole.SELLER);
        Product current = productQuery.authoritative(product);
        validateOwnership.execute(seller, current.getSeller());
        if (!expected.equals(current.getStatus())) {
            throw new DomainValidationException("Invalid product status transition");
        }
        current.setStatus(next);
        return productRepository.update(current);
    }
}