package app.domain.services.logistics;

import app.domain.services.DomainValidations;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Order;
import app.domain.models.OrderItemSnapshot;
import app.domain.models.Shipment;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.exceptions.DomainValidationException;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.ShipmentRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.ShipmentStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.WarehouseOwnerType;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ShipmentRepositoryPort.class)
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepositoryPort shipmentRepository;
    private final OrderRepositoryPort orderRepository;
    private final WarehouseRepositoryPort warehouseRepository;

    public Shipment register(Order order, Warehouse warehouse, User operator) {
        DomainValidations.required(order, "order");
        Order current = orderRepository.findByIdentifier(order)
                .orElseThrow(() -> new DomainValidationException("Order not found"));
        if (!OrderStatus.DISPATCHED.equals(current.getOrderStatus())) {
            throw new DomainValidationException("Order must be dispatched before shipment registration");
        }
        if (current.getItems() == null || current.getItems().stream().noneMatch(OrderItemSnapshot::isPhysical)) {
            throw new DomainValidationException("Shipment requires at least one physical product");
        }
        Warehouse currentWarehouse = warehouseRepository.findByIdentifier(DomainValidations.required(warehouse, "warehouse"))
                .orElseThrow(() -> new DomainValidationException("Warehouse not found"));
        Shipment shipmentQuery = new Shipment();
        shipmentQuery.setOrder(current);
        if (shipmentRepository.findByOrder(shipmentQuery).isPresent()) {
            throw new DomainValidationException("Order already has a shipment");
        }
        validateOperator(currentWarehouse, operator);
        Shipment shipment = new Shipment();
        shipment.setOrder(current);
        shipment.setOriginWarehouse(currentWarehouse);
        shipment.setOperator(operator);
        shipment.setShipmentStatus(ShipmentStatus.IN_PREPARATION);
        return shipmentRepository.save(shipment);
    }

    public Shipment updateStatus(Shipment shipment, ShipmentStatus nextStatus, User requestingUser) {
        DomainValidations.active(requestingUser);
        DomainValidations.required(shipment, "shipment");
        Shipment current = shipmentRepository.findByIdentifier(shipment)
                .orElseThrow(() -> new DomainValidationException("Shipment not found"));
        if (!DomainValidations.sameIdentity(requestingUser, current.getOperator())
                && !UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new DomainValidationException("User cannot update this shipment");
        }
        if (!isNext(current.getShipmentStatus(), DomainValidations.required(nextStatus, "nextStatus"))) {
            throw new DomainValidationException("Invalid shipment status transition");
        }
        current.setShipmentStatus(nextStatus);
        if (ShipmentStatus.DISPATCHED.equals(nextStatus)) {
            current.setDispatchDate(LocalDateTime.now());
        }
        return shipmentRepository.update(current);
    }

    private void validateOperator(Warehouse warehouse, User operator) {
        DomainValidations.required(warehouse, "warehouse");
        DomainValidations.active(operator);
        if (!DomainValidations.sameIdentity(operator, warehouse.getResponsibleUser())) {
            throw new DomainValidationException("Operator is not responsible for warehouse");
        }
        if (WarehouseOwnerType.MARKETPLACE.equals(warehouse.getOwnerType())
                && !UserRole.LOGISTICS_OPERATOR.equals(operator.getRole())) {
            throw new DomainValidationException("Marketplace shipment requires logistics operator");
        }
        if (WarehouseOwnerType.SELLER.equals(warehouse.getOwnerType())
                && !UserRole.SELLER.equals(operator.getRole())) {
            throw new DomainValidationException("Seller shipment requires seller operator");
        }
    }

    private boolean isNext(ShipmentStatus current, ShipmentStatus next) {
        return (ShipmentStatus.IN_PREPARATION.equals(current) && ShipmentStatus.DISPATCHED.equals(next))
                || (ShipmentStatus.DISPATCHED.equals(current) && ShipmentStatus.IN_TRANSIT.equals(next))
                || (ShipmentStatus.IN_TRANSIT.equals(current) && ShipmentStatus.DELIVERED.equals(next));
    }
}
