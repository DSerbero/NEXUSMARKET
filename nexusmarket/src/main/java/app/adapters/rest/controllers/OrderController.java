package app.adapters.rest.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.adapters.rest.dtos.responses.OrderResponse;
import app.adapters.rest.mappers.OrderRestMapper;
import app.domain.exceptions.InvalidCredentialsException;
import app.domain.models.Order;
import app.domain.models.User;
import app.domain.ports.in.OrderPort;
import lombok.RequiredArgsConstructor;

/**
 * Exposes the order lifecycle over HTTP: consult, confirm payment, dispatch and
 * finalize. The controller resolves the authenticated user from the security
 * context and delegates to {@link OrderPort}; per-operation authorization
 * (buyer vs. logistics operator) is applied by the use-case adapter. Order
 * creation is deliberately not exposed here: it only happens via checkout.
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderPort orderPort;
    private final OrderRestMapper orderRestMapper;

    @GetMapping("/{identifier}")
    public OrderResponse consult(@PathVariable String identifier) {
        return orderRestMapper.toResponse(orderPort.consult(currentUser(), orderReference(identifier)));
    }

    @PostMapping("/{identifier}/payment")
    public OrderResponse confirmPayment(@PathVariable String identifier) {
        return orderRestMapper.toResponse(orderPort.confirmPayment(currentUser(), orderReference(identifier)));
    }

    @PostMapping("/{identifier}/dispatch")
    public OrderResponse dispatch(@PathVariable String identifier) {
        return orderRestMapper.toResponse(orderPort.dispatch(currentUser(), orderReference(identifier)));
    }

    @PostMapping("/{identifier}/finalize")
    public OrderResponse finalizeOrder(@PathVariable String identifier) {
        return orderRestMapper.toResponse(orderPort.finalizeOrder(currentUser(), orderReference(identifier)));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String identifier = authentication == null ? null : authentication.getName();
        if (identifier == null || identifier.isBlank()) {
            throw new InvalidCredentialsException();
        }
        User user = new User();
        user.setIdentifier(identifier);
        return user;
    }

    private Order orderReference(String identifier) {
        Order order = new Order();
        order.setIdentifier(identifier);
        return order;
    }
}