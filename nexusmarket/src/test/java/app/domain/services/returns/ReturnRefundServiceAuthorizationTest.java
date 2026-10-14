package app.domain.services.returns;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.Order;
import app.domain.models.Return;
import app.domain.models.User;
import app.domain.ports.out.InvoiceRepositoryPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.RefundRepositoryPort;
import app.domain.ports.out.ReturnRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class ReturnRefundServiceAuthorizationTest {

    @Test
    void buyerCannotRequestReturnForAnotherBuyersOrder() {
        ReturnRepositoryPort returnRepository = mock(ReturnRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        ReturnService service = new ReturnService(returnRepository, orderRepository);
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
        ReturnService service = new ReturnService(returnRepository, mock(OrderRepositoryPort.class));
        User buyer = new User();
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class, () -> service.approve(buyer, new Return()));
        verifyNoInteractions(returnRepository);
    }

    @Test
    void buyerCannotProcessRefund() {
        ReturnRepositoryPort returnRepository = mock(ReturnRepositoryPort.class);
        RefundRepositoryPort refundRepository = mock(RefundRepositoryPort.class);
        InvoiceRepositoryPort invoiceRepository = mock(InvoiceRepositoryPort.class);
        RefundService service = new RefundService(returnRepository, refundRepository, invoiceRepository);
        User buyer = new User();
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);

        assertThrows(DomainValidationException.class, () -> service.process(buyer, new Return()));
        verifyNoInteractions(returnRepository, refundRepository, invoiceRepository);
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