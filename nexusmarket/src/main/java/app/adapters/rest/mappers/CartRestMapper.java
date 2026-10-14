package app.adapters.rest.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import app.adapters.rest.dtos.responses.CartItemResponse;
import app.adapters.rest.dtos.responses.CartResponse;
import app.domain.models.Cart;
import app.domain.models.CartItem;

/**
 * Maps the {@link Cart} domain model to its REST response. Order mapping lives
 * in {@link OrderRestMapper}, which is shared by checkout and order endpoints.
 */
@Component
public class CartRestMapper {

    public CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream().map(this::toItemResponse).toList();
        return new CartResponse(items, cart.getCreationDate());
    }

    private CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getProduct().getIdentifier(),
                item.getQuantity(),
                item.getProduct().getPrice());
    }
}