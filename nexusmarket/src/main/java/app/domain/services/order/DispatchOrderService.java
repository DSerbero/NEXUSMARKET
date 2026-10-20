package app.domain.services.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Order;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.OrderStatus;
import lombok.RequiredArgsConstructor;

/**
 * Dispatch Order: PAID -> DISPATCHED. Registration of the physical shipment is
 * a separate logistics use case, not embedded here (SDD BR-005).
 */
@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class DispatchOrderService {

    private final OrderRepositoryPort orderRepository;

    public Order execute(Order order) {
        Order current = orderRepository.findByIdentifier(order).orElseThrow();
        if (!OrderStatus.PAID.equals(current.getOrderStatus())) {
            throw new DomainValidationException("Invalid order status transition");
        }
        current.setOrderStatus(OrderStatus.DISPATCHED);
        return orderRepository.update(current);
    }
}