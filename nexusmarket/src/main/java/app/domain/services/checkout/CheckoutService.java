package app.domain.services.checkout;

import app.domain.services.DomainValidations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.Product;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.ports.out.CartRepositoryPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates order checkout ({@code ConfirmCart}) as a single application
 * operation. Extracted from {@code OrderService} so each service keeps one
 * responsibility: checkout orchestrates Buyer, Cart, Product and Order, while
 * {@code OrderService} owns only the order lifecycle.
 *
 * <p>The whole confirmation runs in one transaction: it loads the authoritative
 * cart, snapshots published catalog products, saves the order and clears the
 * cart together, as required by the Persistence and Consistency doc.</p>
 */
@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class CheckoutService {

    private final BuyerRepositoryPort buyerRepository;
    private final CartRepositoryPort cartRepository;
    private final ProductRepositoryPort productRepository;
    private final OrderRepositoryPort orderRepository;

    @Transactional
    public Order confirmCart(Buyer requestingBuyer) {
        DomainValidations.active(requestingBuyer);
        Buyer buyer = buyerRepository.findByIdentifier(requestingBuyer)
            .orElseThrow(() -> new app.domain.exceptions.DomainValidationException("Buyer not found"));
        if (!UserRole.BUYER.equals(buyer.getRole())) {
            throw new app.domain.exceptions.DomainValidationException("Only buyers can create orders");
        }
        if (!BuyerStatus.ENABLED.equals(buyer.getBuyerStatus())) {
            throw new app.domain.exceptions.DomainValidationException("Buyer is not enabled");
        }
        Cart cartQuery = new Cart();
        cartQuery.setBuyer(buyer);
        Cart cart = cartRepository.findByBuyerForUpdate(cartQuery)
            .orElseThrow(() -> new app.domain.exceptions.DomainValidationException("Cart not found"));
        if (!DomainValidations.sameIdentity(buyer, cart.getBuyer())) {
            throw new app.domain.exceptions.DomainValidationException("Cart belongs to another buyer");
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new app.domain.exceptions.DomainValidationException("Cart cannot be empty");
        }
        var orderItems = new ArrayList<OrderItemSnapshot>();
        for (CartItem cartItem : cart.getItems()) {
            Product productReference = DomainValidations.required(cartItem.getProduct(), "cartItem.product");
            Product product = productRepository.findByIdentifier(productReference)
                    .orElseThrow(() -> new app.domain.exceptions.DomainValidationException("Product not found"));
            if (!ProductStatus.PUBLISHED.equals(product.getStatus())) {
                throw new app.domain.exceptions.DomainValidationException(
                        "All order products must be published");
            }
            orderItems.add(new OrderItemSnapshot(product, cartItem.getQuantity()));
        }
        Order order = new Order();
        order.setIdentifier(UUID.randomUUID().toString());
        order.setBuyer(buyer);
        order.setItems(orderItems);
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreationDate(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.update(cart);
        return saved;
    }
}
