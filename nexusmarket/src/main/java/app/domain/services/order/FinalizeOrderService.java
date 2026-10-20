package app.domain.services.order;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.OrderStatus;
import lombok.RequiredArgsConstructor;

/**
 * Finalize Order: DISPATCHED -> DELIVERED_FINALIZED. A 100% digital order may
 * finalize straight from PAID (it never dispatches). Finalization is terminal:
 * it stamps the completion date and the order becomes immutable.
 */
@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class FinalizeOrderService {

    private final OrderRepositoryPort orderRepository;

    public Order execute(Order order) {
        Order current = orderRepository.findByIdentifier(order).orElseThrow();
        boolean digitalOnly = current.getItems() != null && !current.getItems().isEmpty()
                && current.getItems().stream().allMatch(OrderItemSnapshot::isDigital);
        boolean validStatus = OrderStatus.DISPATCHED.equals(current.getOrderStatus())
                || (digitalOnly && OrderStatus.PAID.equals(current.getOrderStatus()));
        if (!validStatus) {
            throw new DomainValidationException("Order cannot be finalized from current status");
        }
        current.setOrderStatus(OrderStatus.DELIVERED_FINALIZED);
        current.setCompletionDate(LocalDateTime.now());
        return orderRepository.update(current);
    }
}