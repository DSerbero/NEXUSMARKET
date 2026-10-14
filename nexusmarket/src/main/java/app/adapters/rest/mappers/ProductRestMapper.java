package app.adapters.rest.mappers;

import org.springframework.stereotype.Component;

import app.adapters.rest.dtos.responses.ProductResponse;
import app.domain.models.DigitalProduct;
import app.domain.models.Product;

@Component
public class ProductRestMapper {

    public ProductResponse toResponse(Product product) {
        String type = product instanceof DigitalProduct ? "DIGITAL" : "PHYSICAL";
        String sellerIdentifier = product.getSeller() == null
                ? null : product.getSeller().getIdentifier();
        String digitalAsset = product instanceof DigitalProduct digitalProduct
                ? digitalProduct.getDigitalAsset() : null;
        return new ProductResponse(
                product.getIdentifier(),
                type,
                product.getPrice(),
                product.getVariants(),
                product.getStatus().getCode(),
                sellerIdentifier,
                digitalAsset);
    }
}