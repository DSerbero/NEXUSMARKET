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
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class ProductServiceBusinessRulesTest {

    @Test
    void publishingAlreadyPublishedProductIsRejected() {
        ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
        ProductService service = new ProductService(repository);
        Seller seller = activeSeller();
        DigitalProduct product = product(seller, ProductStatus.PUBLISHED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));

        assertThrows(DomainValidationException.class, () -> service.publish(seller, product));
    }

    @Test
    void suspendedProductCanBePublishedByItsSeller() {
        ProductRepositoryPort repository = mock(ProductRepositoryPort.class);
        ProductService service = new ProductService(repository);
        Seller seller = activeSeller();
        DigitalProduct product = product(seller, ProductStatus.SUSPENDED);
        when(repository.findByIdentifier(product)).thenReturn(Optional.of(product));
        when(repository.update(product)).thenReturn(product);

        assertEquals(ProductStatus.PUBLISHED, service.publish(seller, product).getStatus());
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