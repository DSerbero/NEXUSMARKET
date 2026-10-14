package app.adapters.rest.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import app.adapters.rest.dtos.responses.CartItemResponse;
import app.adapters.rest.dtos.responses.CartResponse;
import app.adapters.rest.dtos.responses.OrderItemResponse;
import app.adapters.rest.dtos.responses.OrderResponse;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;

/**
 * Maps cart and order domain models to their REST responses. Totals and status
 * codes are derived from domain behavior, never computed by hand here.
 */
@Component
public class CartRestMapper {

    public CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream().map(this::toItemResponse).toList();
        return new CartResponse(items, cart.getCreationDate());
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream().map(this::toItemResponse).toList();
        return new OrderResponse(
                order.getIdentifier(),
                order.getOrderStatus().getCode(),
                order.getCreationDate(),
                items);
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getProduct().getIdentifier(),
                item.getQuantity(),
                item.getProduct().getPrice());
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