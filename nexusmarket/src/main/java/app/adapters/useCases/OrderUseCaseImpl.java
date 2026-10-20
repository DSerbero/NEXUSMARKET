package app.adapters.useCases;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.exceptions.EntityNotFoundException;
import app.domain.models.Order;
import app.domain.models.User;
import app.domain.ports.in.OrderPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateOwnershipService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.services.order.ConfirmOrderPaymentService;
import app.domain.services.order.DispatchOrderService;
import app.domain.services.order.FinalizeOrderService;
import app.domain.valueobjects.UserRole;
import lombok.RequiredArgsConstructor;

/**
 * Thin input adapter for the Order Management use cases. It resolves the
 * authoritative user and order, applies the per-operation authorization from
 * the SDD (owning buyer for consult/pay/finalize, logistics operator for
 * dispatch) using the reusable authorization collaborators, and delegates the
 * lifecycle transition to the corresponding single-operation service; it adds
 * no business rules.
 */
@Service
@ConditionalOnBean({ConfirmOrderPaymentService.class, DispatchOrderService.class, FinalizeOrderService.class})
@RequiredArgsConstructor
public class OrderUseCaseImpl implements OrderPort {

    private final UserRepositoryPort userRepository;
    private final OrderRepositoryPort orderRepository;
    private final ConfirmOrderPaymentService confirmOrderPayment;
    private final DispatchOrderService dispatchOrder;
    private final FinalizeOrderService finalizeOrder;
    private final ValidateActiveUserService validateActiveUser;
    private final ValidateRoleService validateRole;
    private final ValidateOwnershipService validateOwnership;

    @Override
    public Order consult(User requestingUser, Order order) {
        User user = authoritativeUser(requestingUser);
        validateActiveUser.execute(user);
        Order current = authoritativeOrder(order);
        validateOwnership.execute(user, current.getBuyer());
        return current;
    }

    @Override
    public Order confirmPayment(User requestingUser, Order order) {
        User user = authoritativeUser(requestingUser);
        validateActiveUser.execute(user);
        Order current = authoritativeOrder(order);
        validateOwnership.execute(user, current.getBuyer());
        return confirmOrderPayment.execute(current);
    }

    @Override
    public Order dispatch(User requestingUser, Order order) {
        User user = authoritativeUser(requestingUser);
        validateActiveUser.execute(user);
        validateRole.execute(user, UserRole.LOGISTICS_OPERATOR);
        return dispatchOrder.execute(authoritativeOrder(order));
    }

    @Override
    public Order finalizeOrder(User requestingUser, Order order) {
        User user = authoritativeUser(requestingUser);
        validateActiveUser.execute(user);
        Order current = authoritativeOrder(order);
        validateOwnership.execute(user, current.getBuyer());
        return finalizeOrder.execute(current);
    }

    private User authoritativeUser(User probe) {
        return userRepository.findByIdentifier(probe)
                .orElseThrow(() -> new EntityNotFoundException("User"));
    }

    private Order authoritativeOrder(Order probe) {
        return orderRepository.findByIdentifier(probe)
                .orElseThrow(() -> new EntityNotFoundException("Order"));
    }
}