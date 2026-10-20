package app.domain.services.order;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Order;
import app.domain.ports.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Consult Order: loads the authoritative order by identifier. Every lifecycle
 * transition starts from this authoritative read (SDD: never trust a
 * caller-supplied order).
 */
@Service
@ConditionalOnBean(OrderRepositoryPort.class)
@RequiredArgsConstructor
public class ConsultOrderService {

    private final OrderRepositoryPort orderRepository;

    public Order execute(Order order) {
        return orderRepository.findByIdentifier(order).orElseThrow();
    }
}