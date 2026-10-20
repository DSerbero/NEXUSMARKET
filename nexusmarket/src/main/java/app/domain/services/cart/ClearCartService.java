package app.domain.services.cart;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.ports.out.CartRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Clear Cart: empties the buyer's active cart after a successful checkout. The
 * cart is cleared, not deleted (SDD BR-005).
 */
@Service
@ConditionalOnBean(CartRepositoryPort.class)
@RequiredArgsConstructor
public class ClearCartService {

    private final CartRepositoryPort cartRepository;

    @Transactional
    public Cart execute(Buyer buyer) {
        CartSupport.validateBuyer(buyer);
        Cart cart = cartRepository.findByBuyerForUpdate(CartSupport.newCart(buyer))
                .orElseGet(() -> CartSupport.newCart(buyer));
        cart.getItems().clear();
        return cartRepository.update(cart);
    }
}