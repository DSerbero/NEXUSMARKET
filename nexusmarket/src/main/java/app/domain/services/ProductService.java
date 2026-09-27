package app.domain.services;

import java.math.BigDecimal;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.DigitalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.services.ports.ProductRepositoryPort;
import app.domain.valueObjects.ProductStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ProductRepositoryPort.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepositoryPort productRepository;

    public Product register(Seller seller, Product product) {
        DomainValidations.active(seller);
        DomainValidations.required(product, "product");
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new app.domain.services.exceptions.DomainValidationException("price must be greater than zero");
        }
        if (product instanceof DigitalProduct digitalProduct
                && (digitalProduct.getDigitalAsset() == null || digitalProduct.getDigitalAsset().isBlank())) {
            throw new app.domain.services.exceptions.DomainValidationException("digitalAsset is required");
        }
        product.setSeller(seller);
        product.setStatus(ProductStatus.PUBLISHED);
        return productRepository.save(product);
    }

    public Product consult(Product product) {
        return productRepository.findByIdentifier(product).orElseThrow();
    }

    public Product update(Seller seller, Product product) {
        DomainValidations.active(seller);
        DomainValidations.required(product, "product");
        if (!seller.equals(product.getSeller())) {
            throw new app.domain.services.exceptions.DomainValidationException("Seller does not own product");
        }
        return productRepository.update(product);
    }

    public Product publish(Product product) {
        product.setStatus(validateTransition(product, ProductStatus.PUBLISHED));
        return productRepository.update(product);
    }

    public Product suspend(Product product) {
        product.setStatus(validateTransition(product, ProductStatus.SUSPENDED));
        return productRepository.update(product);
    }

    public Product discontinue(Product product) {
        product.setStatus(validateTransition(product, ProductStatus.DISCONTINUED));
        return productRepository.update(product);
    }

    private ProductStatus validateTransition(Product product, ProductStatus nextStatus) {
        DomainValidations.required(product, "product");
        if (ProductStatus.DISCONTINUED.equals(product.getStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Discontinued product cannot change status");
        }
        return nextStatus;
    }
}
