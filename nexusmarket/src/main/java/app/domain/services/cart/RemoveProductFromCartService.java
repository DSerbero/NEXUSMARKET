package app.domain.services.cart;

import java.util.Iterator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.Product;
import app.domain.ports.out.CartRepositoryPort;
import app.domain.services.DomainValidations;
import lombok.RequiredArgsConstructor;

/**
 * Remove Product from Cart: decrements the product quantity and removes the
 * line when it reaches zero. Fails when the product is not in the cart (never a
 * silent no-op).
 */
@Service
@ConditionalOnBean(CartRepositoryPort.class)
@RequiredArgsConstructor
public class RemoveProductFromCartService {

    private final CartRepositoryPort cartRepository;

    @Transactional
    public Cart execute(Buyer buyer, Product product) {
        CartSupport.validateBuyer(buyer);
        DomainValidations.required(product, "product");
        Cart cart = cartRepository.findByBuyerForUpdate(CartSupport.newCart(buyer)).orElseThrow();
        Iterator<CartItem> items = cart.getItems().iterator();
        while (items.hasNext()) {
            CartItem item = items.next();
            if (product.getIdentifier().equals(item.getProduct().getIdentifier())) {
                if (item.decrement()) {
                    items.remove();
                }
                return cartRepository.update(cart);
            }
        }
        throw new app.domain.exceptions.DomainValidationException("Product is not in cart");
    }
}