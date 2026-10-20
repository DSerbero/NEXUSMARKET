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
 * Discontinue Product: (PUBLISHED | SUSPENDED) -> DISCONTINUED, performed by the
 * owning seller. A discontinued product is terminal.
 */
@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class DiscontinueProductService {

    private final ProductRepositoryPort productRepository;
    private final ProductQueryService productQuery;
    private final ValidateActiveUserService validateActiveUser;
    private final ValidateRoleService validateRole;
    private final ValidateOwnershipService validateOwnership;

    public Product execute(Seller seller, Product product) {
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
}