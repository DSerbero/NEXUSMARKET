package app.domain.services.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import app.domain.models.Buyer;
import app.domain.models.Cart;
import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.Product;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.ports.out.CartRepositoryPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.ProductRepositoryPort;
import app.domain.services.cart.CartService;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.ProductStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:checkout;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class CheckoutPersistenceIntegrationTest {

    @Autowired
    private BuyerRepositoryPort buyerRepository;

    @Autowired
    private CartRepositoryPort cartRepository;

    @Autowired
    private OrderRepositoryPort orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @MockitoBean
    private ProductRepositoryPort productRepository;

    @Test
    void persistingAndConfirmingCartKeepsQuantityAndOrderSnapshotConsistent() {
        Buyer buyer = new Buyer();
        buyer.setIdentifier(UUID.randomUUID().toString());
        buyer.setFullName("Buyer Test");
        buyer.setEmail(buyer.getIdentifier() + "@example.test");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        buyer.setPrimaryAddress("Test address");
        buyer = buyerRepository.save(buyer);

        DigitalProduct catalogProduct = new DigitalProduct();
        catalogProduct.setIdentifier(UUID.randomUUID().toString());
        catalogProduct.setPrice(new BigDecimal("1500.00"));
        catalogProduct.setStatus(ProductStatus.PUBLISHED);
        catalogProduct.setDigitalAsset("asset-ref");
        when(productRepository.findByIdentifier(any(Product.class))).thenReturn(Optional.of(catalogProduct));

        cartService.addProduct(buyer, catalogProduct);
        cartService.addProduct(buyer, catalogProduct);

        Cart cartQuery = new Cart();
        cartQuery.setBuyer(buyer);
        Cart storedCart = cartRepository.findByBuyer(cartQuery).orElseThrow();
        assertEquals(2, storedCart.getItems().get(0).getQuantity());

        Order confirmed = orderService.confirmCart(buyer);
        Order orderQuery = new Order();
        orderQuery.setIdentifier(confirmed.getIdentifier());
        Order storedOrder = orderRepository.findByIdentifier(orderQuery).orElseThrow();
        Cart emptiedCart = cartRepository.findByBuyer(cartQuery).orElseThrow();

        assertEquals(2, storedOrder.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("1500.00"), storedOrder.getItems().get(0).getUnitPrice());
        assertEquals("COP", storedOrder.getItems().get(0).getCurrencyCode());
        assertEquals(0, new BigDecimal("3000.00").compareTo(storedOrder.getItems().get(0).getLineTotal()));
        assertTrue(emptiedCart.getItems().isEmpty());
    }
}