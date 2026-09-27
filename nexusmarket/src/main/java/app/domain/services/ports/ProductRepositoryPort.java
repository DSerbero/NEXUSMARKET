package app.domain.services.ports;

import java.util.List;
import java.util.Optional;

import app.domain.models.Product;

public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findByIdentifier(Product product);
    List<Product> findBySeller(Product product);
    Product update(Product product);
}
