package app.adapters.rest.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import app.adapters.rest.dtos.responses.OrderItemResponse;
import app.adapters.rest.dtos.responses.OrderResponse;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;

/**
 * Maps the {@link Order} domain model to its REST response. Shared by the
 * checkout endpoint (CartController) and the order lifecycle endpoints
 * (OrderController) so the order shape is defined in exactly one place.
 */
@Component
public class OrderRestMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(this::toItemResponse).toList();
        return new OrderResponse(
                order.getIdentifier(),
                order.getOrderStatus().getCode(),
                order.getCreationDate(),
                items);
    }

    private OrderItemResponse toItemResponse(OrderItemSnapshot snapshot) {
        return new OrderItemResponse(
                snapshot.getProductIdentifier(),
                snapshot.getProductType().getCode(),
                snapshot.getUnitPrice(),
                snapshot.getQuantity(),
                snapshot.getLineTotal(),
                snapshot.getCurrencyCode());
    }
}