package app.domain.services.cart;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.BuyerStatus;

/**
 * Shared cart preconditions: only an active, commercially enabled buyer may
 * operate a cart, and a cart reference is always anchored to its buyer.
 */
final class CartSupport {

    private CartSupport() {
    }

    static void validateBuyer(Buyer buyer) {
        DomainValidations.active(buyer);
        if (!BuyerStatus.ENABLED.equals(buyer.getBuyerStatus())) {
            throw new app.domain.exceptions.DomainValidationException("Buyer is not enabled");
        }
    }

    static Cart newCart(Buyer buyer) {
        Cart cart = new Cart();
        cart.setBuyer(buyer);
        return cart;
    }
}