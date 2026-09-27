package app.domain.services;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.Product;
import app.domain.services.ports.CartRepositoryPort;
import app.domain.services.ports.ProductRepositoryPort;
import app.domain.valueObjects.BuyerStatus;
import app.domain.valueObjects.ProductStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(CartRepositoryPort.class)
@RequiredArgsConstructor
public class CartService {

    private final CartRepositoryPort cartRepository;
    private final ProductRepositoryPort productRepository;

    public Cart addProduct(Buyer buyer, Product product) {
        validateBuyer(buyer);
        Product currentProduct = productRepository.findByIdentifier(product).orElseThrow();
        if (!ProductStatus.PUBLISHED.equals(currentProduct.getStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Product is not published");
        }
        Cart cart = cartRepository.findByBuyer(newCart(buyer)).orElseGet(() -> newCart(buyer));
        if (cart.getCreationDate() == null) {
            cart.setCreationDate(LocalDateTime.now());
        }
        cart.getItems().add(currentProduct);
        return cartRepository.save(cart);
    }

    public Cart removeProduct(Buyer buyer, Product product) {
        validateBuyer(buyer);
        Cart cart = cartRepository.findByBuyer(newCart(buyer)).orElseThrow();
        if (!cart.getItems().remove(product)) {
            throw new app.domain.services.exceptions.DomainValidationException("Product is not in cart");
        }
        return cartRepository.update(cart);
    }

    public Cart consult(Buyer buyer) {
        validateBuyer(buyer);
        return cartRepository.findByBuyer(newCart(buyer)).orElseThrow();
    }

    public Cart clear(Buyer buyer) {
        Cart cart = consult(buyer);
        cart.getItems().clear();
        return cartRepository.update(cart);
    }

    private void validateBuyer(Buyer buyer) {
        DomainValidations.active(buyer);
        if (!BuyerStatus.ENABLED.equals(buyer.getBuyerStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Buyer is not enabled");
        }
    }

    private Cart newCart(Buyer buyer) {
        Cart cart = new Cart();
        cart.setBuyer(buyer);
        return cart;
    }
}
