package app.domain.services.product;

import app.domain.services.DomainValidations;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.DigitalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepositoryPort productRepository;

    public Product register(Seller seller, Product product) {
        DomainValidations.active(seller);
        requireSeller(seller);
        DomainValidations.required(product, "product");
        validateProduct(product);
        product.setSeller(seller);
        product.setStatus(ProductStatus.PUBLISHED);
        return productRepository.save(product);
    }

    public Product consult(User requestingUser, Product product) {
        DomainValidations.active(requestingUser);
        Product current = authoritative(product);
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
        Product current = authoritative(product);
        if (!ProductStatus.PUBLISHED.equals(current.getStatus())) {
            throw new DomainValidationException("Product not found");
        }
        return current;
    }

    public Product update(Seller seller, Product product) {
        DomainValidations.active(seller);
        requireSeller(seller);
        Product current = authoritative(product);
        requireOwner(seller, current);
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

    public Product publish(Seller seller, Product product) {
        return updateStatus(seller, product, ProductStatus.SUSPENDED, ProductStatus.PUBLISHED);
    }

    public Product suspend(Seller seller, Product product) {
        return updateStatus(seller, product, ProductStatus.PUBLISHED, ProductStatus.SUSPENDED);
    }

    public Product discontinue(Seller seller, Product product) {
        DomainValidations.active(seller);
        requireSeller(seller);
        Product current = authoritative(product);
        requireOwner(seller, current);
        if (!ProductStatus.PUBLISHED.equals(current.getStatus())
                && !ProductStatus.SUSPENDED.equals(current.getStatus())) {
            throw new DomainValidationException("Product cannot be discontinued from its current status");
        }
        current.setStatus(ProductStatus.DISCONTINUED);
        return productRepository.update(current);
    }

    private Product updateStatus(Seller seller, Product product, ProductStatus expected, ProductStatus next) {
        DomainValidations.active(seller);
        requireSeller(seller);
        Product current = authoritative(product);
        requireOwner(seller, current);
        if (!expected.equals(current.getStatus())) {
            throw new DomainValidationException("Invalid product status transition");
        }
        current.setStatus(next);
        return productRepository.update(current);
    }

    private Product authoritative(Product product) {
        DomainValidations.required(product, "product");
        return productRepository.findByIdentifier(product)
                .orElseThrow(() -> new DomainValidationException("Product not found"));
    }

    private void requireOwner(Seller seller, Product product) {
        if (!DomainValidations.sameIdentity(seller, product.getSeller())) {
            throw new DomainValidationException("Seller does not own product");
        }
    }

    private void requireSeller(Seller seller) {
        if (!UserRole.SELLER.equals(seller.getRole())) {
            throw new DomainValidationException("Only sellers can manage products");
        }
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
