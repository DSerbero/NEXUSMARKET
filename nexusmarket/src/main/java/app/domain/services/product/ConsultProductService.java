package app.domain.services.product;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Product;
import app.domain.models.User;
import app.domain.services.DomainValidations;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Consult Product: an active user reads a product. Published products are
 * public; anything else is visible only to its seller or to ADMIN/SUPERVISOR.
 */
@Service
@ConditionalOnBean(ProductQueryService.class)
@RequiredArgsConstructor
public class ConsultProductService {

    private final ProductQueryService productQuery;
    private final ValidateActiveUserService validateActiveUser;

    public Product execute(User requestingUser, Product product) {
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
}