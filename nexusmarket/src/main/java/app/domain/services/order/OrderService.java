package app.domain.services.order;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.OrderStatus;
import lombok.RequiredArgsConstructor;

/**
 * Owns the order lifecycle: consultation and the PENDING_PAYMENT -> PAID ->
 * DISPATCHED -> DELIVERED_FINALIZED transitions. Checkout orchestration lives in
 * {@code CheckoutService} so this class keeps a single responsibility.
 */
@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepositoryPort orderRepository;

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
                && current.getItems().stream().allMatch(OrderItemSnapshot::isDigital);
        boolean validStatus = OrderStatus.DISPATCHED.equals(current.getOrderStatus())
            || (digitalOnly && OrderStatus.PAID.equals(current.getOrderStatus()));
        if (!validStatus) {
            throw new DomainValidationException(
                    "Order cannot be finalized from current status");
        }
        current.setOrderStatus(OrderStatus.DELIVERED_FINALIZED);
        current.setCompletionDate(LocalDateTime.now());
        return orderRepository.update(current);
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (!expected.equals(order.getOrderStatus())) {
            throw new DomainValidationException("Invalid order status transition");
        }
    }
}
