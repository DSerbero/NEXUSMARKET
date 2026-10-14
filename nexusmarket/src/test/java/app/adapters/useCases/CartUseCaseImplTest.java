package app.adapters.useCases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.EntityNotFoundException;
import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.PhysicalProduct;
import app.domain.models.Product;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.services.cart.CartService;
import app.domain.services.checkout.CheckoutService;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class CartUseCaseImplTest {

    private final BuyerRepositoryPort buyerRepository = mock(BuyerRepositoryPort.class);
    private final CartService cartService = mock(CartService.class);
    private final CheckoutService checkoutService = mock(CheckoutService.class);
    private final CartUseCaseImpl useCase =
            new CartUseCaseImpl(buyerRepository, cartService, checkoutService);

    @Test
    void consultResolvesAuthoritativeBuyerBeforeDelegating() {
        Buyer probe = probe();
        Buyer stored = activeBuyer();
        when(buyerRepository.findByIdentifier(probe)).thenReturn(Optional.of(stored));
        Cart cart = new Cart();
        when(cartService.consult(stored)).thenReturn(cart);

        assertSame(cart, useCase.consult(probe));
        verify(cartService).consult(stored);
    }

    @Test
    void addProductDelegatesWithAuthoritativeBuyer() {
        Buyer probe = probe();
        Buyer stored = activeBuyer();
        when(buyerRepository.findByIdentifier(probe)).thenReturn(Optional.of(stored));
        Product product = new PhysicalProduct();
        product.setIdentifier("product-1");
        Cart cart = new Cart();
        when(cartService.addProduct(same(stored), same(product))).thenReturn(cart);

        assertSame(cart, useCase.addProduct(probe, product));
    }

    @Test
    void confirmDelegatesToCheckout() {
        Buyer probe = probe();
        Buyer stored = activeBuyer();
        when(buyerRepository.findByIdentifier(probe)).thenReturn(Optional.of(stored));
        Order order = new Order();
        when(checkoutService.confirmCart(stored)).thenReturn(order);

        assertSame(order, useCase.confirm(probe));
    }

    @Test
    void unknownBuyerIsReportedAsNotFound() {
        when(buyerRepository.findByIdentifier(any())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> useCase.consult(probe()));
    }

    @Test
    void removeProductDoesNotBypassTheAuthoritativeLoad() {
        Buyer probe = probe();
        Buyer stored = activeBuyer();
        when(buyerRepository.findByIdentifier(probe)).thenReturn(Optional.of(stored));
        Product product = new DigitalProduct();
        product.setIdentifier("product-1");
        Cart cart = new Cart();
        when(cartService.removeProduct(same(stored), same(product))).thenReturn(cart);

        assertSame(cart, useCase.removeProduct(probe, product));
        assertEquals("buyer-1", stored.getIdentifier());
    }

    private Buyer probe() {
        Buyer probe = new Buyer();
        probe.setIdentifier("buyer-1");
        return probe;
    }

    private Buyer activeBuyer() {
        Buyer buyer = new Buyer();
        buyer.setIdentifier("buyer-1");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        return buyer;
    }
}