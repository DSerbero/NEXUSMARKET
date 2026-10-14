package application.adapters.useCases.cart;

import application.domain.services.DomainValidations;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Product;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.valueobjects.BuyerStatus;
import application.domain.valueobjects.ProductStatus;
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
            throw new application.domain.exceptions.DomainValidationException("Product is not published");
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
            throw new application.domain.exceptions.DomainValidationException("Product is not in cart");
        }
        return cartRepository.update(cart);
    }

    public Cart consult(Buyer buyer) {
        validateBuyer(buyer);
        return cartRepository.findByBuyer(newCart(buyer)).orElseGet(() -> {
            Cart cart = newCart(buyer);
            cart.setCreationDate(LocalDateTime.now());
            return cart;
        });
    }

    public Cart clear(Buyer buyer) {
        Cart cart = consult(buyer);
        cart.getItems().clear();
        return cartRepository.update(cart);
    }

    private void validateBuyer(Buyer buyer) {
        DomainValidations.active(buyer);
        if (!BuyerStatus.ENABLED.equals(buyer.getBuyerStatus())) {
            throw new application.domain.exceptions.DomainValidationException("Buyer is not enabled");
        }
    }

    private Cart newCart(Buyer buyer) {
        Cart cart = new Cart();
        cart.setBuyer(buyer);
        return cart;
    }
}
