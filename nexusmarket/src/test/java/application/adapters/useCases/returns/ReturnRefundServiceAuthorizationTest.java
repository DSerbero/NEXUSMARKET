package application.adapters.useCases.returns;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.RefundRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
import application.domain.valueobjects.BuyerStatus;
import application.domain.valueobjects.OrderStatus;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

class ReturnRefundServiceAuthorizationTest {

    @Test
    void buyerCannotRequestReturnForAnotherBuyersOrder() {
        ReturnRepositoryPort returnRepository = mock(ReturnRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        ReturnRefundService service = new ReturnRefundService(returnRepository,
                mock(RefundRepositoryPort.class), orderRepository, mock(InvoiceRepositoryPort.class));
        Buyer requester = buyer("buyer-1");
        Buyer orderOwner = buyer("buyer-2");
        Order order = new Order();
        order.setBuyer(orderOwner);
        order.setOrderStatus(OrderStatus.DELIVERED_FINALIZED);
        when(orderRepository.findByIdentifier(order)).thenReturn(Optional.of(order));

        assertThrows(DomainValidationException.class, () -> service.request(requester, order, "Damaged"));
        verifyNoInteractions(returnRepository);
    }

    @Test
    void buyerCannotApproveReturn() {
        ReturnRepositoryPort returnRepository = mock(ReturnRepositoryPort.class);
        ReturnRefundService service = new ReturnRefundService(returnRepository,
                mock(RefundRepositoryPort.class), mock(OrderRepositoryPort.class), mock(InvoiceRepositoryPort.class));
        User buyer = new User();
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class, () -> service.approve(buyer, new Return()));
        verifyNoInteractions(returnRepository);
    }

    private Buyer buyer(String identifier) {
        Buyer buyer = new Buyer();
        buyer.setIdentifier(identifier);
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        return buyer;
    }
}