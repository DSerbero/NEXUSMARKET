package app.adapters.rest.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import app.adapters.rest.dtos.requests.AddCartItemRequest;
import app.adapters.rest.dtos.responses.CartResponse;
import app.adapters.rest.dtos.responses.OrderResponse;
import app.adapters.rest.mappers.CartRestMapper;
import app.adapters.rest.mappers.OrderRestMapper;
import app.domain.exceptions.InvalidCredentialsException;
import app.domain.ports.in.CartPort;
import app.domain.models.Buyer;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the Cart Management use cases over HTTP. The controller resolves the
 * authenticated buyer from the security context, maps request/response DTOs and
 * delegates to {@link CartPort}; no business rule lives here.
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartPort cartPort;
    private final CartRestMapper cartRestMapper;
    private final OrderRestMapper orderRestMapper;

    @GetMapping
    public CartResponse consult() {
        return cartRestMapper.toCartResponse(cartPort.consult(currentBuyer()));
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        Product product = productReference(request.productId());
        return cartRestMapper.toCartResponse(cartPort.addProduct(currentBuyer(), product));
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@PathVariable String productId) {
        Product product = productReference(productId);
        return cartRestMapper.toCartResponse(cartPort.removeProduct(currentBuyer(), product));
    }

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse confirm() {
        return orderRestMapper.toResponse(cartPort.confirm(currentBuyer()));
    }

    private Buyer currentBuyer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identifier = authentication == null ? null : authentication.getName();
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidCredentialsException();
        }
        Buyer buyer = new Buyer();
        buyer.setIdentifier(identifier);
        return buyer;
    }

    private Product productReference(String productId) {
        Product product = new PhysicalProduct();
        product.setIdentifier(productId);
        return product;
    }
}