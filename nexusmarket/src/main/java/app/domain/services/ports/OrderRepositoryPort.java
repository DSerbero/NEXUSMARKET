package app.domain.services.ports;

import java.util.List;
import java.util.Optional;

import app.domain.models.Order;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findByIdentifier(Order order);
    List<Order> findByBuyer(Order order);
    Order update(Order order);
}
