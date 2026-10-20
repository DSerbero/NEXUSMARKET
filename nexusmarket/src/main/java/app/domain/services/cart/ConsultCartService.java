package app.domain.services.cart;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.ports.out.CartRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Consult Cart: returns the buyer's current cart, or a fresh empty cart (with a
 * new timestamp) when none exists yet. No error for a missing cart.
 */
@Service
@ConditionalOnBean(CartRepositoryPort.class)
@RequiredArgsConstructor
public class ConsultCartService {

    private final CartRepositoryPort cartRepository;

    public Cart execute(Buyer buyer) {
        CartSupport.validateBuyer(buyer);
        return cartRepository.findByBuyer(CartSupport.newCart(buyer)).orElseGet(() -> {
            Cart cart = CartSupport.newCart(buyer);
            cart.setCreationDate(LocalDateTime.now());
            return cart;
        });
    }
}