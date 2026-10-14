package app.adapters.persistence.mongodb.mappers;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.models.Seller;
import app.domain.valueobjects.ProductStatus;
import app.adapters.persistence.mongodb.documents.ProductDocument;

@Component
public class ProductDocumentMapper {

    public ProductDocument toDocument(Product product) {
        ProductDocument document = new ProductDocument();
        document.setIdentifier(requiredIdentifier(product));
        document.setProductType(typeOf(product));
        document.setPrice(product.getPrice());
        document.setVariants(product.getVariants() == null
                ? new ArrayList<>() : new ArrayList<>(product.getVariants()));
        document.setStatus(product.getStatus() == null ? null : product.getStatus().getCode());
        Seller seller = product.getSeller();
        document.setSellerIdentifier(seller == null ? null : seller.getIdentifier());
        if (product instanceof DigitalProduct digitalProduct) {
            document.setDigitalAsset(digitalProduct.getDigitalAsset());
        }
        return document;
    }

    public Product toDomain(ProductDocument document) {
        Product product = switch (document.getProductType()) {
            case "PHYSICAL" -> new PhysicalProduct();
            case "DIGITAL" -> new DigitalProduct();
            default -> throw new DomainValidationException("Unsupported persisted product type");
        };
        product.setIdentifier(document.getIdentifier());
        product.setPrice(document.getPrice());
        product.setVariants(document.getVariants() == null
                ? new ArrayList<>() : new ArrayList<>(document.getVariants()));
        product.setStatus(statusFrom(document.getStatus()));
        if (document.getSellerIdentifier() != null) {
            Seller seller = new Seller();
            seller.setIdentifier(document.getSellerIdentifier());
            product.setSeller(seller);
        }
        if (product instanceof DigitalProduct digitalProduct) {
            digitalProduct.setDigitalAsset(document.getDigitalAsset());
        }
        return product;
    }

    private String requiredIdentifier(Product product) {
        if (product.getIdentifier() == null || product.getIdentifier().isBlank()) {
            throw new DomainValidationException("Product identifier is required");
        }
        return product.getIdentifier();
    }

    private String typeOf(Product product) {
        if (product instanceof PhysicalProduct) {
            return "PHYSICAL";
        }
        if (product instanceof DigitalProduct) {
            return "DIGITAL";
        }
        throw new DomainValidationException("Unsupported product type");
    }

    private ProductStatus statusFrom(String code) {
        if (ProductStatus.PUBLISHED.getCode().equals(code)) {
            return ProductStatus.PUBLISHED;
        }
        if (ProductStatus.SUSPENDED.getCode().equals(code)) {
            return ProductStatus.SUSPENDED;
        }
        if (ProductStatus.DISCONTINUED.getCode().equals(code)) {
            return ProductStatus.DISCONTINUED;
        }
        throw new DomainValidationException("Unsupported persisted product status");
    }
}