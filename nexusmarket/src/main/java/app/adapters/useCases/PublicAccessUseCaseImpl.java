package app.adapters.useCases;

import app.domain.models.Product;
import app.domain.ports.in.PublicAccessPort;
import app.domain.services.product.ConsultPublishedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(ConsultPublishedProductService.class)
@RequiredArgsConstructor
public class PublicAccessUseCaseImpl implements PublicAccessPort {

    private final ConsultPublishedProductService consultPublishedProduct;

    @Override
    public Product consultPublishedProduct(Product product) {
        return consultPublishedProduct.execute(product);
    }
}