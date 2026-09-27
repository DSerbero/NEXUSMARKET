package app.domain.services.ports;

import java.util.Optional;

import app.domain.models.Seller;

public interface SellerRepositoryPort {
    Seller save(Seller seller);
    Optional<Seller> findByIdentifier(Seller seller);
    Optional<Seller> findByEmail(Seller seller);
    boolean existsByEmail(Seller seller);
    Seller update(Seller seller);
}
