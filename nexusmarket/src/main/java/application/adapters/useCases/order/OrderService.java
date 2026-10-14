package application.adapters.useCases.order;

import application.domain.services.DomainValidations;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.DigitalProduct;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.BuyerStatus;
import application.domain.valueobjects.OrderStatus;
import application.domain.valueobjects.ProductStatus;
import application.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepositoryPort orderRepository;

    public Order create(Buyer buyer, Cart cart) {
        DomainValidations.active(buyer);
        if (!UserRole.BUYER.equals(buyer.getRole())) {
            throw new application.domain.exceptions.DomainValidationException("Only buyers can create orders");
        }
        if (!BuyerStatus.ENABLED.equals(buyer.getBuyerStatus())) {
            throw new application.domain.exceptions.DomainValidationException("Buyer is not enabled");
        }
        DomainValidations.required(cart, "cart");
        if (!DomainValidations.sameIdentity(buyer, cart.getBuyer())) {
            throw new application.domain.exceptions.DomainValidationException("Cart belongs to another buyer");
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new application.domain.exceptions.DomainValidationException("Cart cannot be empty");
        }
        for (Product product : cart.getItems()) {
            if (product == null || !ProductStatus.PUBLISHED.equals(product.getStatus())) {
                throw new application.domain.exceptions.DomainValidationException(
                        "All order products must be published");
            }
        }
        Order order = new Order();
        order.setBuyer(buyer);
        order.setItems(new java.util.ArrayList<>(cart.getItems()));
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreationDate(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Order consult(Order order) {
        return orderRepository.findByIdentifier(order).orElseThrow();
    }

    public Order confirmPayment(Order order) {
        Order current = consult(order);
        requireStatus(current, OrderStatus.PENDING_PAYMENT);
        current.setOrderStatus(OrderStatus.PAID);
        return orderRepository.update(current);
    }

    public Order dispatch(Order order) {
        Order current = consult(order);
        requireStatus(current, OrderStatus.PAID);
        current.setOrderStatus(OrderStatus.DISPATCHED);
        return orderRepository.update(current);
    }

    public Order finalizeOrder(Order order) {
        Order current = consult(order);
        boolean digitalOnly = current.getItems() != null && !current.getItems().isEmpty()
            && current.getItems().stream().allMatch(DigitalProduct.class::isInstance);
        boolean validStatus = OrderStatus.DISPATCHED.equals(current.getOrderStatus())
            || (digitalOnly && OrderStatus.PAID.equals(current.getOrderStatus()));
        if (!validStatus) {
            throw new application.domain.exceptions.DomainValidationException(
                    "Order cannot be finalized from current status");
        }
        current.setOrderStatus(OrderStatus.DELIVERED_FINALIZED);
        current.setCompletionDate(LocalDateTime.now());
        return orderRepository.update(current);
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (!expected.equals(order.getOrderStatus())) {
            throw new application.domain.exceptions.DomainValidationException("Invalid order status transition");
        }
    }
}
