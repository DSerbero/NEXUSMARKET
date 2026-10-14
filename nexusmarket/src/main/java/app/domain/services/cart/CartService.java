package app.domain.services.cart;

import app.domain.services.DomainValidations;

import java.util.Iterator;
import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.Product;
import app.domain.models.CartItem;
import app.domain.ports.out.CartRepositoryPort;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnBean(CartRepositoryPort.class)
@RequiredArgsConstructor
public class CartService {

    private final CartRepositoryPort cartRepository;
    private final ProductRepositoryPort productRepository;

    @Transactional
    public Cart addProduct(Buyer buyer, Product product) {
        validateBuyer(buyer);
        Product currentProduct = productRepository.findByIdentifier(product).orElseThrow();
        if (!ProductStatus.PUBLISHED.equals(currentProduct.getStatus())) {
            throw new app.domain.exceptions.DomainValidationException("Product is not published");
        }
        Cart query = newCart(buyer);
        var stored = cartRepository.findByBuyerForUpdate(query);
        Cart cart = stored.orElseGet(() -> newCart(buyer));
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

    @Transactional
    public Cart removeProduct(Buyer buyer, Product product) {
        validateBuyer(buyer);
        DomainValidations.required(product, "product");
        Cart cart = cartRepository.findByBuyerForUpdate(newCart(buyer)).orElseThrow();
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

    public Cart consult(Buyer buyer) {
        validateBuyer(buyer);
        return cartRepository.findByBuyer(newCart(buyer)).orElseGet(() -> {
            Cart cart = newCart(buyer);
            cart.setCreationDate(LocalDateTime.now());
            return cart;
        });
    }

    @Transactional
    public Cart clear(Buyer buyer) {
        validateBuyer(buyer);
        Cart cart = cartRepository.findByBuyerForUpdate(newCart(buyer)).orElseGet(() -> newCart(buyer));
        cart.getItems().clear();
        return cartRepository.update(cart);
    }

    private void validateBuyer(Buyer buyer) {
        DomainValidations.active(buyer);
        if (!BuyerStatus.ENABLED.equals(buyer.getBuyerStatus())) {
            throw new app.domain.exceptions.DomainValidationException("Buyer is not enabled");
        }
    }

    private Cart newCart(Buyer buyer) {
        Cart cart = new Cart();
        cart.setBuyer(buyer);
        return cart;
    }
}
