package application.adapters.useCases.logistics;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.DigitalProduct;
import application.domain.models.Order;
import application.domain.models.Warehouse;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.OrderStatus;

class ShipmentServiceBusinessRulesTest {

    @Test
    void digitalOnlyOrderCannotHaveShipment() {
        ShipmentRepositoryPort shipmentRepository = mock(ShipmentRepositoryPort.class);
        OrderRepositoryPort orderRepository = mock(OrderRepositoryPort.class);
        WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);
        ShipmentService service = new ShipmentService(shipmentRepository, orderRepository, warehouseRepository);
        Order order = new Order();
        order.setOrderStatus(OrderStatus.DISPATCHED);
        order.setItems(List.of(new DigitalProduct()));
        when(orderRepository.findByIdentifier(order)).thenReturn(Optional.of(order));

        assertThrows(DomainValidationException.class,
                () -> service.register(order, new Warehouse(), null));
        verifyNoInteractions(shipmentRepository, warehouseRepository);
    }
}