package app.domain.services.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.CartItem;
import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.Product;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.ports.out.CartRepositoryPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

class CheckoutServiceTest {

    @Test
    void confirmationUsesCurrentProductSnapshotAndClearsAuthoritativeCart() {
        Buyer buyer = activeBuyer("buyer-1");
        DigitalProduct cartProduct = digitalProduct("product-1", BigDecimal.ONE, ProductStatus.SUSPENDED);
        Cart cart = cart(buyer, cartProduct, 3);
        DigitalProduct currentProduct = digitalProduct("product-1", new BigDecimal("12.50"), ProductStatus.PUBLISHED);
        BuyerRepositoryPort buyerRepository = mock(BuyerRepositoryPort.class);
        CartRepositoryPort cartRepository = mock(CartRepositoryPort.class);
        ProductRepositoryPort productRepository = mock(ProductRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        CheckoutService service = service(buyerRepository, cartRepository, productRepository, orderRepository);
        when(buyerRepository.findByIdentifier(buyer)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByBuyerForUpdate(any(Cart.class))).thenReturn(Optional.of(cart));
        when(productRepository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(currentProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = service.confirmCart(buyer);

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getOrderStatus());
        assertNotNull(order.getIdentifier());
        assertTrue(cart.getItems().isEmpty());
        OrderItemSnapshot item = order.getItems().get(0);
        assertEquals("product-1", item.getProductIdentifier());
        assertEquals(3, item.getQuantity());
        assertEquals(new BigDecimal("12.50"), item.getUnitPrice());
        assertEquals("COP", item.getCurrencyCode());
        assertEquals(new BigDecimal("37.50"), item.getLineTotal());
        verify(orderRepository).save(order);
        verify(cartRepository).update(cart);
    }

    @Test
    void unpublishedProductPreventsOrderAndLeavesCartUntouched() {
        Buyer buyer = activeBuyer("buyer-1");
        Cart cart = cart(buyer, digitalProduct("product-1", BigDecimal.TEN, ProductStatus.PUBLISHED), 1);
        BuyerRepositoryPort buyerRepository = mock(BuyerRepositoryPort.class);
        CartRepositoryPort cartRepository = mock(CartRepositoryPort.class);
        ProductRepositoryPort productRepository = mock(ProductRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        CheckoutService service = service(buyerRepository, cartRepository, productRepository, orderRepository);
        when(buyerRepository.findByIdentifier(buyer)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByBuyerForUpdate(any(Cart.class))).thenReturn(Optional.of(cart));
        when(productRepository.findByIdentifier(any(Product.class)))
                .thenReturn(Optional.of(digitalProduct("product-1", BigDecimal.TEN, ProductStatus.SUSPENDED)));

        assertThrows(DomainValidationException.class, () -> service.confirmCart(buyer));

        assertEquals(1, cart.getItems().size());
        verifyNoInteractions(orderRepository);
    }

    @Test
    void cartReturnedForAnotherBuyerIsRejected() {
        Buyer buyer = activeBuyer("buyer-1");
        Buyer otherBuyer = activeBuyer("buyer-2");
        Cart cart = cart(otherBuyer, digitalProduct("product-1", BigDecimal.TEN, ProductStatus.PUBLISHED), 1);
        BuyerRepositoryPort buyerRepository = mock(BuyerRepositoryPort.class);
        CartRepositoryPort cartRepository = mock(CartRepositoryPort.class);
        ProductRepositoryPort productRepository = mock(ProductRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        CheckoutService service = service(buyerRepository, cartRepository, productRepository, orderRepository);
        when(buyerRepository.findByIdentifier(buyer)).thenReturn(Optional.of(buyer));
        when(cartRepository.findByBuyerForUpdate(any(Cart.class))).thenReturn(Optional.of(cart));

        assertThrows(DomainValidationException.class, () -> service.confirmCart(buyer));

        verifyNoInteractions(productRepository, orderRepository);
    }

    private CheckoutService service(BuyerRepositoryPort buyerRepository, CartRepositoryPort cartRepository,
            ProductRepositoryPort productRepository, OrderRepositoryPort orderRepository) {
        return new CheckoutService(buyerRepository, cartRepository, productRepository, orderRepository);
    }

    private Buyer activeBuyer(String identifier) {
        Buyer buyer = new Buyer();
        buyer.setIdentifier(identifier);
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        return buyer;
    }

    private Cart cart(Buyer buyer, Product product, int quantity) {
        Cart cart = new Cart();
        cart.setBuyer(buyer);
        cart.getItems().add(new CartItem(product, quantity));
        return cart;
    }

    private DigitalProduct digitalProduct(String identifier, BigDecimal price, ProductStatus status) {
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier(identifier);
        product.setPrice(price);
        product.setStatus(status);
        product.setDigitalAsset("asset-" + identifier);
        return product;
    }
}
