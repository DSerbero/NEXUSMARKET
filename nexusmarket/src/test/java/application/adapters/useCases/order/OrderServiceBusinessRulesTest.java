package application.adapters.useCases.order;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.DigitalProduct;
import application.domain.models.Order;
import application.domain.models.PhysicalProduct;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.BuyerStatus;
import application.domain.valueobjects.OrderStatus;
import application.domain.valueobjects.ProductStatus;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

class OrderServiceBusinessRulesTest {

    @Test
    void rejectsOrderContainingUnpublishedProduct() {
        OrderRepositoryPort repository = mock(OrderRepositoryPort.class);
        OrderService service = new OrderService(repository);
        Buyer buyer = activeBuyer();
        DigitalProduct product = new DigitalProduct();
        product.setStatus(ProductStatus.SUSPENDED);
        Cart cart = new Cart();
        cart.getItems().add(product);

        assertThrows(DomainValidationException.class, () -> service.create(buyer, cart));
    }

    @Test
    void cannotFinalizeOrderBeforeDispatch() {
        OrderRepositoryPort repository = mock(OrderRepositoryPort.class);
        OrderService service = new OrderService(repository);
        Order paidOrder = new Order();
        paidOrder.setOrderStatus(OrderStatus.PAID);
        when(repository.findByIdentifier(paidOrder)).thenReturn(Optional.of(paidOrder));

        assertThrows(DomainValidationException.class, () -> service.finalizeOrder(paidOrder));
    }

    @Test
    void digitalOnlyOrderCanFinalizeDirectlyAfterPayment() {
        OrderRepositoryPort repository = mock(OrderRepositoryPort.class);
        OrderService service = new OrderService(repository);
        Order paidOrder = new Order();
        paidOrder.setOrderStatus(OrderStatus.PAID);
        paidOrder.getItems().add(new DigitalProduct());
        when(repository.findByIdentifier(paidOrder)).thenReturn(Optional.of(paidOrder));
        when(repository.update(paidOrder)).thenReturn(paidOrder);

        assertEquals(OrderStatus.DELIVERED_FINALIZED, service.finalizeOrder(paidOrder).getOrderStatus());
    }

    @Test
    void orderCannotBeCreatedFromAnotherBuyersCart() {
        OrderService service = new OrderService(mock(OrderRepositoryPort.class));
        Buyer buyer = activeBuyer();
        Buyer cartOwner = activeBuyer();
        cartOwner.setIdentifier("buyer-2");
        Cart cart = new Cart();
        cart.setBuyer(cartOwner);
        cart.getItems().add(new PhysicalProduct());
        cart.getItems().get(0).setStatus(ProductStatus.PUBLISHED);

        assertThrows(DomainValidationException.class, () -> service.create(buyer, cart));
    }

    private Buyer activeBuyer() {
        Buyer buyer = new Buyer();
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        return buyer;
    }
}