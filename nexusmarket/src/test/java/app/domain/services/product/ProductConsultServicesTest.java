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
import app.domain.models.User;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

/** Read-side catalog rules: authorized and public consultation. */
class ProductConsultServicesTest {

    private final ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
    private final ConsultProductService consult = new ConsultProductService(
            new ProductQueryService(repository), new ValidateActiveUserService());
    private final ConsultPublishedProductService consultPublished =
            new ConsultPublishedProductService(new ProductQueryService(repository));

    @Test
    void sellerCannotConsultAnotherSellersPrivateProduct() {
        Seller owner = activeSeller("seller-owner");
        Seller stranger = activeSeller("seller-stranger");
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setSeller(owner);
        product.setStatus(ProductStatus.SUSPENDED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> consult.execute(stranger, product));
    }

    @Test
    void consultPublishedRejectsNonPublishedProduct() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setStatus(ProductStatus.SUSPENDED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> consultPublished.execute(product));
    }

    @Test
    void consultPublishedReturnsPublishedProduct() {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("product-1");
        product.setStatus(ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertEquals(ProductStatus.PUBLISHED, consultPublished.execute(product).getStatus());
    }

    private Seller activeSeller(String identifier) {
        Seller seller = new Seller();
        seller.setIdentifier(identifier);
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        return seller;
    }
}