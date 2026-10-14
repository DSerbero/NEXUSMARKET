package app.domain.services.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.models.DigitalProduct;
import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ProductStatus;

class OrderServiceLifecycleTest {

    @Test
    void digitalOnlyOrderCanFinalizeDirectlyAfterPayment() {
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        OrderService service = new OrderService(orderRepository);
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PAID);
        order.getItems().add(new OrderItemSnapshot(
                digitalProduct("product-1", BigDecimal.TEN, ProductStatus.PUBLISHED), 1));
        when(orderRepository.findByIdentifier(order)).thenReturn(Optional.of(order));
        when(orderRepository.update(order)).thenReturn(order);

        assertEquals(OrderStatus.DELIVERED_FINALIZED, service.finalizeOrder(order).getOrderStatus());
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
