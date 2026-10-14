package app.adapters.useCases;

import app.domain.models.Product;
import app.domain.ports.in.PublicAccessPort;
import app.domain.services.product.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ProductCatalogService.class)
@RequiredArgsConstructor
public class PublicAccessUseCaseImpl implements PublicAccessPort {

    private final ProductCatalogService productCatalogService;

    @Override
    public Product consultPublishedProduct(Product product) {
        return productCatalogService.consultPublished(product);
    }
}