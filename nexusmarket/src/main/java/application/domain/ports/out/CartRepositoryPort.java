package application.domain.ports.out;

import java.util.Optional;

import application.domain.models.Cart;

public interface CartRepositoryPort {
    Cart save(Cart cart);
    Optional<Cart> findByBuyer(Cart cart);
    Cart update(Cart cart);
}
