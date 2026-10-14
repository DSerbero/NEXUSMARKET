package app.adapters.useCases;

import app.domain.models.Product;
import app.domain.ports.in.PublicAccessPort;
import app.domain.services.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ProductService.class)
@RequiredArgsConstructor
public class PublicAccessUseCaseImpl implements PublicAccessPort {

    private final ProductService productService;

    @Override
    public Product consultPublishedProduct(Product product) {
        return productService.consultPublished(product);
    }
}