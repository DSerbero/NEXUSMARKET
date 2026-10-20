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
import app.domain.services.cart.AddProductToCartService;
import app.domain.services.cart.ConsultCartService;
import app.domain.services.cart.RemoveProductFromCartService;
import app.domain.services.checkout.CheckoutService;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class CartUseCaseImplTest {

    private final BuyerRepositoryPort buyerRepository = mock(BuyerRepositoryPort.class);
    private final AddProductToCartService addProductToCart = mock(AddProductToCartService.class);
    private final RemoveProductFromCartService removeProductFromCart = mock(RemoveProductFromCartService.class);
    private final ConsultCartService consultCart = mock(ConsultCartService.class);
    private final CheckoutService checkoutService = mock(CheckoutService.class);
    private final CartUseCaseImpl useCase =
            new CartUseCaseImpl(buyerRepository, addProductToCart, removeProductFromCart, consultCart, checkoutService);

    @Test
    void consultResolvesAuthoritativeBuyerBeforeDelegating() {
        Buyer probe = probe();
        Buyer stored = activeBuyer();
        when(buyerRepository.findByIdentifier(probe)).thenReturn(Optional.of(stored));
        Cart cart = new Cart();
        when(consultCart.execute(stored)).thenReturn(cart);

        assertSame(cart, useCase.consult(probe));
        verify(consultCart).execute(stored);
    }

    @Test
    void addProductDelegatesWithAuthoritativeBuyer() {
        Buyer probe = probe();
        Buyer stored = activeBuyer();
        when(buyerRepository.findByIdentifier(probe)).thenReturn(Optional.of(stored));
        Product product = new PhysicalProduct();
        product.setIdentifier("product-1");
        Cart cart = new Cart();
        when(addProductToCart.execute(same(stored), same(product))).thenReturn(cart);

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
        when(removeProductFromCart.execute(same(stored), same(product))).thenReturn(cart);

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