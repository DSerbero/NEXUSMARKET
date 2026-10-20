package app.domain.services.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Order;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.OrderStatus;
import lombok.RequiredArgsConstructor;

/**
 * Confirm Order Payment: PENDING_PAYMENT -> PAID. Invoicing is a separate
 * subdomain and is not triggered here (SDD BR-005).
 */
@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class ConfirmOrderPaymentService {

    private final OrderRepositoryPort orderRepository;

    public Order execute(Order order) {
        Order current = orderRepository.findByIdentifier(order).orElseThrow();
        requireStatus(current, OrderStatus.PENDING_PAYMENT);
        current.setOrderStatus(OrderStatus.PAID);
        return orderRepository.update(current);
    }

    private void requireStatus(Order order, OrderStatus expected) {
        if (!expected.equals(order.getOrderStatus())) {
            throw new DomainValidationException("Invalid order status transition");
        }
    }
}