package app.adapters.useCases;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.EntityNotFoundException;
import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.Order;
import app.domain.models.Product;
import app.domain.ports.in.CartPort;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.services.cart.CartService;
import app.domain.services.checkout.CheckoutService;
import lombok.RequiredArgsConstructor;

/**
 * Thin input adapter for the Cart Management use cases. It resolves the
 * authoritative {@link Buyer} from the identity carried by the request (the
 * security layer extracted it from the JWT) and delegates to the domain
 * services; it adds no business rules of its own.
 */
@Service
@ConditionalOnBean({CartService.class, CheckoutService.class})
@RequiredArgsConstructor
public class CartUseCaseImpl implements CartPort {

    private final BuyerRepositoryPort buyerRepository;
    private final CartService cartService;
    private final CheckoutService checkoutService;

    @Override
    public Cart consult(Buyer requestingBuyer) {
        return cartService.consult(authoritative(requestingBuyer));
    }

    @Override
    public Cart addProduct(Buyer requestingBuyer, Product product) {
        return cartService.addProduct(authoritative(requestingBuyer), product);
    }

    @Override
    public Cart removeProduct(Buyer requestingBuyer, Product product) {
        return cartService.removeProduct(authoritative(requestingBuyer), product);
    }

    @Override
    public Order confirm(Buyer requestingBuyer) {
        return checkoutService.confirmCart(authoritative(requestingBuyer));
    }

    private Buyer authoritative(Buyer requestingBuyer) {
        return buyerRepository.findByIdentifier(requestingBuyer)
                .orElseThrow(() -> new EntityNotFoundException("Buyer"));
    }
}