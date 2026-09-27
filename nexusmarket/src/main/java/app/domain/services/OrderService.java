package app.domain.services;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.Order;
import app.domain.services.ports.OrderRepositoryPort;
import app.domain.valueObjects.OrderStatus;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepositoryPort orderRepository;

    public Order create(Buyer buyer, Cart cart) {
        DomainValidations.active(buyer);
        DomainValidations.required(cart, "cart");
        if (cart.getItems().isEmpty()) {
            throw new app.domain.services.exceptions.DomainValidationException("Cart cannot be empty");
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
        if (!OrderStatus.DISPATCHED.equals(current.getOrderStatus())
                && !OrderStatus.PAID.equals(current.getOrderStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Order cannot be finalized from current status");
        }
        current.setOrderStatus(OrderStatus.DELIVERED_FINALIZED);
        current.setCompletionDate(LocalDateTime.now());
        return orderRepository.update(current);
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (!expected.equals(order.getOrderStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Invalid order status transition");
        }
    }
}
