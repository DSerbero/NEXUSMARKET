package app.domain.services.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateOwnershipService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

/** Write-side catalog rules: registration and update of commercial data. */
class ProductCatalogWriteServicesTest {

    private final ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
    private final RegisterProductService register = new RegisterProductService(
            repository, new ValidateActiveUserService(), new ValidateRoleService());
    private final UpdateProductService update = new UpdateProductService(
            repository, new ProductQueryService(repository),
            new ValidateActiveUserService(), new ValidateRoleService(), new ValidateOwnershipService());

    @Test
    void registerPublishesValidDigitalProduct() {
        Seller seller = activeSeller();
        DigitalProduct product = new DigitalProduct();
        product.setPrice(new BigDecimal("10.00"));
        product.setDigitalAsset("asset-ref");
        when(repository.save(product)).thenReturn(product);

        assertEquals(ProductStatus.PUBLISHED, register.execute(seller, product).getStatus());
        assertEquals(seller, product.getSeller());
    }

    @Test
    void registerRejectsNonPositivePrice() {
        Seller seller = activeSeller();
        DigitalProduct product = new DigitalProduct();
        product.setPrice(BigDecimal.ZERO);
        product.setDigitalAsset("asset-ref");

        assertThrows(DomainValidationException.class, () -> register.execute(seller, product));
    }

    @Test
    void registerRejectsDigitalProductWithoutAsset() {
        Seller seller = activeSeller();
        DigitalProduct product = new DigitalProduct();
        product.setPrice(new BigDecimal("10.00"));

        assertThrows(DomainValidationException.class, () -> register.execute(seller, product));
    }

    @Test
    void updateRejectsDiscontinuedProduct() {
        Seller seller = activeSeller();
        DigitalProduct current = new DigitalProduct();
        current.setIdentifier("product-1");
        current.setSeller(seller);
        current.setStatus(ProductStatus.DISCONTINUED);
        when(repository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(current));

        DigitalProduct change = new DigitalProduct();
        change.setIdentifier("product-1");
        change.setPrice(new BigDecimal("20.00"));
        change.setDigitalAsset("asset-ref");

        assertThrows(DomainValidationException.class, () -> update.execute(seller, change));
    }

    @Test
    void updateRejectsProductTypeChange() {
        Seller seller = activeSeller();
        DigitalProduct current = new DigitalProduct();
        current.setIdentifier("product-1");
        current.setSeller(seller);
        current.setStatus(ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(current));

        PhysicalProduct change = new PhysicalProduct();
        change.setIdentifier("product-1");
        change.setPrice(new BigDecimal("20.00"));

        assertThrows(DomainValidationException.class, () -> update.execute(seller, change));
    }

    private Seller activeSeller() {
        Seller seller = new Seller();
        seller.setIdentifier("seller-1");
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        return seller;
    }
}