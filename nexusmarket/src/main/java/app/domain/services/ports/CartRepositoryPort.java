package app.domain.services.ports;

import java.util.Optional;

import app.domain.models.Cart;

public interface CartRepositoryPort {
    Cart save(Cart cart);
    Optional<Cart> findByBuyer(Cart cart);
    Cart update(Cart cart);
}
