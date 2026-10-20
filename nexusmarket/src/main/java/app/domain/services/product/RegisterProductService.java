package app.domain.services.product;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.DomainValidations;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Register Product: an active seller publishes a new, valid product. Price and
 * digital-asset invariants are enforced by {@link ProductSupport}.
 */
@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class RegisterProductService {

    private final ProductRepositoryPort productRepository;
    private final ValidateActiveUserService validateActiveUser;
    private final ValidateRoleService validateRole;

    public Product execute(Seller seller, Product product) {
        validateActiveUser.execute(seller);
        validateRole.execute(seller, UserRole.SELLER);
        DomainValidations.required(product, "product");
        ProductSupport.validateProduct(product);
        product.setSeller(seller);
        product.setStatus(ProductStatus.PUBLISHED);
        return productRepository.save(product);
    }
}