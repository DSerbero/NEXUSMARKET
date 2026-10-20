package app.domain.services.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
import app.domain.models.Seller;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateOwnershipService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

/** Product status state machine: publish, suspend, discontinue. */
class ProductLifecycleServicesTest {

    private final ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
    private final PublishProductService publish = new PublishProductService(
            repository, new ProductQueryService(repository),
            new ValidateActiveUserService(), new ValidateRoleService(), new ValidateOwnershipService());
    private final SuspendProductService suspend = new SuspendProductService(
            repository, new ProductQueryService(repository),
            new ValidateActiveUserService(), new ValidateRoleService(), new ValidateOwnershipService());
    private final DiscontinueProductService discontinue = new DiscontinueProductService(
            repository, new ProductQueryService(repository),
            new ValidateActiveUserService(), new ValidateRoleService(), new ValidateOwnershipService());

    @Test
    void publishingAlreadyPublishedProductIsRejected() {
        Seller seller = activeSeller();
        DigitalProduct product = product(seller, ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> publish.execute(seller, product));
    }

    @Test
    void suspendedProductCanBePublishedByItsSeller() {
        Seller seller = activeSeller();
        DigitalProduct product = product(seller, ProductStatus.SUSPENDED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));
        when(repository.update(product)).thenReturn(product);

        assertEquals(ProductStatus.PUBLISHED, publish.execute(seller, product).getStatus());
    }

    @Test
    void publishedProductCanBeSuspendedByItsSeller() {
        Seller seller = activeSeller();
        DigitalProduct product = product(seller, ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));
        when(repository.update(product)).thenReturn(product);

        assertEquals(ProductStatus.SUSPENDED, suspend.execute(seller, product).getStatus());
    }

    @Test
    void discontinuedProductCannotBeDiscontinuedAgain() {
        Seller seller = activeSeller();
        DigitalProduct product = product(seller, ProductStatus.DISCONTINUED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> discontinue.execute(seller, product));
    }

    private Seller activeSeller() {
        Seller seller = new Seller();
        seller.setIdentifier("seller-1");
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        return seller;
    }

    private DigitalProduct product(Seller seller, ProductStatus status) {
        DigitalProduct product = new DigitalProduct();
        product.setSeller(seller);
        product.setStatus(status);
        return product;
    }
}