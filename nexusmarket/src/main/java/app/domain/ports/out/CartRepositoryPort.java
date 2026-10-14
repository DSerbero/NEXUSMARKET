package app.domain.ports.out;

import java.util.Optional;

import app.domain.models.Cart;

public interface CartRepositoryPort {
    Cart save(Cart cart);
    Optional<Cart> findByBuyer(Cart cart);
    Optional<Cart> findByBuyerForUpdate(Cart cart);
    Cart update(Cart cart);
}
