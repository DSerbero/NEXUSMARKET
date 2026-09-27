package app.domain.services.ports;

import java.util.Optional;

import app.domain.models.Buyer;

public interface BuyerRepositoryPort {
    Buyer save(Buyer buyer);
    Optional<Buyer> findByIdentifier(Buyer buyer);
    Optional<Buyer> findByEmail(Buyer buyer);
    boolean existsByEmail(Buyer buyer);
    Buyer update(Buyer buyer);
}
