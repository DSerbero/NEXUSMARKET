package app.domain.ports.in;

import app.domain.models.Product;

public interface PublicAccessPort {
    Product consultPublishedProduct(Product product);
}