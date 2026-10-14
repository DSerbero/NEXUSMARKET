package app.domain.services.logistics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.DigitalProduct;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.Order;
import app.domain.models.Warehouse;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.ShipmentRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.OrderStatus;

class ShipmentServiceBusinessRulesTest {

    @Test
    void digitalOnlyOrderCannotHaveShipment() {
        ShipmentRepositoryPort shipmentRepository = mock(ShipmentRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);
        ShipmentService service = new ShipmentService(shipmentRepository, orderRepository, warehouseRepository);
        Order order = new Order();
        order.setOrderStatus(OrderStatus.DISPATCHED);
        DigitalProduct product = new DigitalProduct();
        product.setIdentifier("digital-1");
        product.setPrice(BigDecimal.ONE);
        product.setDigitalAsset("asset-1");
        order.setItems(List.of(new OrderItemSnapshot(product, 1)));
        when(orderRepository.findByIdentifier(order)).thenReturn(Optional.of(order));

        assertThrows(DomainValidationException.class,
                () -> service.register(order, new Warehouse(), null));
        verifyNoInteractions(shipmentRepository, warehouseRepository);
    }
}