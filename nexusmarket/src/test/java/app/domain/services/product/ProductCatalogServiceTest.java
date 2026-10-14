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

class ProductCatalogServiceTest {

    private final ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
    private final ProductCatalogService service = new ProductCatalogService(repository,
            new ProductQueryService(repository),
            new ValidateActiveUserService(),
            new ValidateRoleService(),
            new ValidateOwnershipService());

    @Test
    void registerPublishesValidDigitalProduct() {
        Seller seller = activeSeller();
        DigitalProduct product = new DigitalProduct();
        product.setPrice(new BigDecimal("10.00"));
        product.setDigitalAsset("asset-ref");
        when(repository.save(product)).thenReturn(product);

        assertEquals(ProductStatus.PUBLISHED, service.register(seller, product).getStatus());
        assertEquals(seller, product.getSeller());
    }

    @Test
    void registerRejectsNonPositivePrice() {
        Seller seller = activeSeller();
        DigitalProduct product = new DigitalProduct();
        product.setPrice(BigDecimal.ZERO);
        product.setDigitalAsset("asset-ref");

        assertThrows(DomainValidationException.class, () -> service.register(seller, product));
    }

    @Test
    void registerRejectsDigitalProductWithoutAsset() {
        Seller seller = activeSeller();
        DigitalProduct product = new DigitalProduct();
        product.setPrice(new BigDecimal("10.00"));

        assertThrows(DomainValidationException.class, () -> service.register(seller, product));
    }

    @Test
    void updateRejectsDiscontinuedProduct() {
        Seller seller = activeSeller();
        DigitalProduct current = new DigitalProduct();
        current.setIdentifier("product-1");
        current.setSeller(seller);
        current.setStatus(ProductStatus.DISCONTINUED);
        when(repository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(current));

        DigitalProduct update = new DigitalProduct();
        update.setIdentifier("product-1");
        update.setPrice(new BigDecimal("20.00"));
        update.setDigitalAsset("asset-ref");

        assertThrows(DomainValidationException.class, () -> service.update(seller, update));
    }

    @Test
    void updateRejectsProductTypeChange() {
        Seller seller = activeSeller();
        DigitalProduct current = new DigitalProduct();
        current.setIdentifier("product-1");
        current.setSeller(seller);
        current.setStatus(ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(current));

        PhysicalProduct update = new PhysicalProduct();
        update.setIdentifier("product-1");
        update.setPrice(new BigDecimal("20.00"));

        assertThrows(DomainValidationException.class, () -> service.update(seller, update));
    }

    @Test
    void consultPublishedRejectsNonPublishedProduct() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setStatus(ProductStatus.SUSPENDED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> service.consultPublished(product));
    }

    @Test
    void consultPublishedReturnsPublishedProduct() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setStatus(ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertEquals(ProductStatus.PUBLISHED, service.consultPublished(product).getStatus());
    }

    @Test
    void sellerCannotConsultAnotherSellersPrivateProduct() {
        Seller owner = activeSeller();
        owner.setIdentifier("seller-owner");
        Seller stranger = activeSeller();
        stranger.setIdentifier("seller-stranger");
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setSeller(owner);
        product.setStatus(ProductStatus.SUSPENDED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> service.consult(stranger, product));
    }

    private Seller activeSeller() {
        Seller seller = new Seller();
        seller.setIdentifier("seller-1");
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        return seller;
    }
}