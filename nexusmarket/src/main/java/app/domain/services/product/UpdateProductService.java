package app.domain.services.product;

import java.util.ArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
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
 * Update Product: the owning seller refreshes commercial data (price, variants
 * and, for digital products, the asset). Discontinued products are immutable
 * and the product type cannot change.
 */
@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class UpdateProductService {

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
        if (ProductStatus.DISCONTINUED.equals(current.getStatus())) {
            throw new DomainValidationException("Discontinued product cannot be updated");
        }
        ProductSupport.validateProduct(product);
        if (current.getClass() != product.getClass()) {
            throw new DomainValidationException("Product type cannot be changed");
        }
        current.setPrice(product.getPrice());
        current.setVariants(product.getVariants() == null
                ? new ArrayList<>() : new ArrayList<>(product.getVariants()));
        if (current instanceof DigitalProduct currentDigital && product instanceof DigitalProduct updatedDigital) {
            currentDigital.setDigitalAsset(updatedDigital.getDigitalAsset());
        }
        return productRepository.update(current);
    }
}