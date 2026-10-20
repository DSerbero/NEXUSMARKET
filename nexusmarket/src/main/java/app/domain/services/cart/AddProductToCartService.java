package app.domain.services.cart;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.Product;
import app.domain.ports.out.CartRepositoryPort;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.valueobjects.ProductStatus;
import lombok.RequiredArgsConstructor;

/**
 * Add Product to Cart: revalidates the product is PUBLISHED, then adds it (or
 * increments its quantity) to the buyer's single active cart, creating the cart
 * on first use. The cart row is locked for the mutation.
 */
@Service
@ConditionalOnBean(CartRepositoryPort.class)
@RequiredArgsConstructor
public class AddProductToCartService {

    private final CartRepositoryPort cartRepository;
    private final ProductRepositoryPort productRepository;

    @Transactional
    public Cart execute(Buyer buyer, Product product) {
        CartSupport.validateBuyer(buyer);
        Product currentProduct = productRepository.findByIdentifier(product).orElseThrow();
        if (!ProductStatus.PUBLISHED.equals(currentProduct.getStatus())) {
            throw new app.domain.exceptions.DomainValidationException("Product is not published");
        }
        var stored = cartRepository.findByBuyerForUpdate(CartSupport.newCart(buyer));
        Cart cart = stored.orElseGet(() -> CartSupport.newCart(buyer));
        if (cart.getCreationDate() == null) {
            cart.setCreationDate(LocalDateTime.now());
        }
        CartItem existing = cart.getItems().stream()
                .filter(item -> currentProduct.getIdentifier().equals(item.getProduct().getIdentifier()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            cart.getItems().add(new CartItem(currentProduct, 1));
        } else {
            existing.increment();
        }
        return stored.isPresent() ? cartRepository.update(cart) : cartRepository.save(cart);
    }
}