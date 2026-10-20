package app.adapters.useCases;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.EntityNotFoundException;
import app.domain.exceptions.UnauthorizedOperationException;
import app.domain.models.Buyer;
import app.domain.models.Order;
import app.domain.models.User;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.UserRepositoryPort;
import app.domain.services.authorization.ValidateActiveUserService;
import app.domain.services.authorization.ValidateOwnershipService;
import app.domain.services.authorization.ValidateRoleService;
import app.domain.services.order.ConfirmOrderPaymentService;
import app.domain.services.order.DispatchOrderService;
import app.domain.services.order.FinalizeOrderService;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class OrderUseCaseImplTest {

    private final UserRepositoryPort userRepository = mock(UserRepositoryPort.class);
    private final OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
    private final ConfirmOrderPaymentService confirmOrderPayment = mock(ConfirmOrderPaymentService.class);
    private final DispatchOrderService dispatchOrder = mock(DispatchOrderService.class);
    private final FinalizeOrderService finalizeOrderService = mock(FinalizeOrderService.class);
    private final OrderUseCaseImpl useCase = new OrderUseCaseImpl(
            userRepository, orderRepository, confirmOrderPayment, dispatchOrder, finalizeOrderService,
            new ValidateActiveUserService(), new ValidateRoleService(), new ValidateOwnershipService());

    @Test
    void buyerConsultsOwnOrder() {
        Buyer buyer = activeBuyer();
        Order stored = order(buyer);
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(buyer));
        when(orderRepository.findByIdentifier(any())).thenReturn(Optional.of(stored));

        assertSame(stored, useCase.consult(probe("buyer-1"), probeOrder()));
    }

    @Test
    void buyerCannotConsultForeignOrder() {
        Buyer owner = activeBuyer();
        owner.setIdentifier("buyer-2");
        Order stored = order(owner);
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(activeBuyer()));
        when(orderRepository.findByIdentifier(any())).thenReturn(Optional.of(stored));

        assertThrows(UnauthorizedOperationException.class,
                () -> useCase.consult(probe("buyer-1"), probeOrder()));
    }

    @Test
    void inactiveUserIsRejected() {
        Buyer buyer = activeBuyer();
        buyer.setStatus(UserStatus.BLOCKED);
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(buyer));

        assertThrows(UnauthorizedOperationException.class,
                () -> useCase.consult(probe("buyer-1"), probeOrder()));
    }

    @Test
    void confirmPaymentDelegatesToOrderService() {
        Buyer buyer = activeBuyer();
        Order stored = order(buyer);
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(buyer));
        when(orderRepository.findByIdentifier(any())).thenReturn(Optional.of(stored));
        Order paid = order(buyer);
        paid.setOrderStatus(app.domain.valueobjects.OrderStatus.PAID);
        when(confirmOrderPayment.execute(stored)).thenReturn(paid);

        assertSame(paid, useCase.confirmPayment(probe("buyer-1"), probeOrder()));
    }

    @Test
    void onlyLogisticsOperatorCanDispatch() {
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(activeBuyer()));

        assertThrows(UnauthorizedOperationException.class,
                () -> useCase.dispatch(probe("buyer-1"), probeOrder()));
    }

    @Test
    void logisticsOperatorDispatchesOrder() {
        User operator = new User();
        operator.setIdentifier("operator-1");
        operator.setRole(UserRole.LOGISTICS_OPERATOR);
        operator.setStatus(UserStatus.ACTIVE);
        Order stored = order(activeBuyer());
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(operator));
        when(orderRepository.findByIdentifier(any())).thenReturn(Optional.of(stored));
        when(dispatchOrder.execute(stored)).thenReturn(stored);

        assertSame(stored, useCase.dispatch(probe("operator-1"), probeOrder()));
    }

    @Test
    void unknownOrderIsReportedAsNotFound() {
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.of(activeBuyer()));
        when(orderRepository.findByIdentifier(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> useCase.consult(probe("buyer-1"), probeOrder()));
    }

    @Test
    void unknownUserIsReportedAsNotFound() {
        when(userRepository.findByIdentifier(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> useCase.consult(probe("buyer-1"), probeOrder()));
        verify(orderRepository, org.mockito.Mockito.never()).findByIdentifier(any());
    }

    private User probe(String identifier) {
        User user = new User();
        user.setIdentifier(identifier);
        return user;
    }

    private Order probeOrder() {
        Order order = new Order();
        order.setIdentifier("order-1");
        return order;
    }

    private Buyer activeBuyer() {
        Buyer buyer = new Buyer();
        buyer.setIdentifier("buyer-1");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        return buyer;
    }

    private Order order(Buyer buyer) {
        Order order = new Order();
        order.setIdentifier("order-1");
        order.setBuyer(buyer);
        order.setOrderStatus(app.domain.valueobjects.OrderStatus.PENDING_PAYMENT);
        return order;
    }
}