package app.domain.services.product;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateOwnershipService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Catalog operations over products: registration, consultation and update of
 * commercial data. Status transitions are a different reason to change and
 * live in {@link ProductLifecycleService} (SRP).
 */
@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductRepositoryPort productRepository;
    private final ProductQueryService productQuery;
    private final ValidateActiveUserService validateActiveUser;
    private final ValidateRoleService validateRole;
    private final ValidateOwnershipService validateOwnership;

    public Product register(Seller seller, Product product) {
        validateActiveUser.execute(seller);
        validateRole.execute(seller, UserRole.SELLER);
        DomainValidations.required(product, "product");
        validateProduct(product);
        product.setSeller(seller);
        product.setStatus(ProductStatus.PUBLISHED);
        return productRepository.save(product);
    }

    public Product consult(User requestingUser, Product product) {
        validateActiveUser.execute(requestingUser);
        Product current = productQuery.authoritative(product);
        boolean publicProduct = ProductStatus.PUBLISHED.equals(current.getStatus());
        boolean privilegedReader = UserRole.ADMIN.equals(requestingUser.getRole())
                || UserRole.SUPERVISOR.equals(requestingUser.getRole());
        if (!publicProduct && !privilegedReader
                && !DomainValidations.sameIdentity(requestingUser, current.getSeller())) {
            throw new DomainValidationException("User cannot consult this product");
        }
        return current;
    }

    public Product consultPublished(Product product) {
        Product current = productQuery.authoritative(product);
        if (!ProductStatus.PUBLISHED.equals(current.getStatus())) {
            throw new DomainValidationException("Product not found");
        }
        return current;
    }

    public Product update(Seller seller, Product product) {
        validateActiveUser.execute(seller);
        validateRole.execute(seller, UserRole.SELLER);
        Product current = productQuery.authoritative(product);
        validateOwnership.execute(seller, current.getSeller());
        if (ProductStatus.DISCONTINUED.equals(current.getStatus())) {
            throw new DomainValidationException("Discontinued product cannot be updated");
        }
        validateProduct(product);
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

    private void validateProduct(Product product) {
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