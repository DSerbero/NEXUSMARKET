package app.domain.services;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Order;
import app.domain.models.Shipment;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.services.ports.OrderRepositoryPort;
import app.domain.services.ports.ShipmentRepositoryPort;
import app.domain.valueObjects.OrderStatus;
import app.domain.valueObjects.ShipmentStatus;
import app.domain.valueObjects.UserRole;
import app.domain.valueObjects.WarehouseOwnerType;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(ShipmentRepositoryPort.class)
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepositoryPort shipmentRepository;
    private final OrderRepositoryPort orderRepository;

    public Shipment register(Order order, Warehouse warehouse, User operator) {
        Order current = orderRepository.findByIdentifier(order).orElseThrow();
        if (!OrderStatus.PAID.equals(current.getOrderStatus())) {
            throw new app.domain.services.exceptions.DomainValidationException("Order must be paid before shipment registration");
        }
        validateOperator(warehouse, operator);
        Shipment shipment = new Shipment();
        shipment.setOrder(current);
        shipment.setOriginWarehouse(warehouse);
        shipment.setOperator(operator);
        shipment.setShipmentStatus(ShipmentStatus.IN_PREPARATION);
        return shipmentRepository.save(shipment);
    }

    public Shipment updateStatus(Shipment shipment, ShipmentStatus nextStatus, User requestingUser) {
        DomainValidations.active(requestingUser);
        if (!requestingUser.equals(shipment.getOperator())
                && !UserRole.ADMIN.equals(requestingUser.getRole())) {
            throw new app.domain.services.exceptions.DomainValidationException("User cannot update this shipment");
        }
        if (!isNext(shipment.getShipmentStatus(), nextStatus)) {
            throw new app.domain.services.exceptions.DomainValidationException("Invalid shipment status transition");
        }
        shipment.setShipmentStatus(nextStatus);
        if (ShipmentStatus.DISPATCHED.equals(nextStatus)) {
            shipment.setDispatchDate(LocalDateTime.now());
        }
        return shipmentRepository.update(shipment);
    }

    private void validateOperator(Warehouse warehouse, User operator) {
        DomainValidations.required(warehouse, "warehouse");
        DomainValidations.required(operator, "operator");
        if (!operator.equals(warehouse.getResponsibleUser())) {
            throw new app.domain.services.exceptions.DomainValidationException("Operator is not responsible for warehouse");
        }
        if (WarehouseOwnerType.MARKETPLACE.equals(warehouse.getOwnerType())
                && !UserRole.LOGISTICS_OPERATOR.equals(operator.getRole())) {
            throw new app.domain.services.exceptions.DomainValidationException("Marketplace shipment requires logistics operator");
        }
        if (WarehouseOwnerType.SELLER.equals(warehouse.getOwnerType())
                && !UserRole.SELLER.equals(operator.getRole())) {
            throw new app.domain.services.exceptions.DomainValidationException("Seller shipment requires seller operator");
        }
    }

    private boolean isNext(ShipmentStatus current, ShipmentStatus next) {
        return (ShipmentStatus.IN_PREPARATION.equals(current) && ShipmentStatus.DISPATCHED.equals(next))
                || (ShipmentStatus.DISPATCHED.equals(current) && ShipmentStatus.IN_TRANSIT.equals(next))
                || (ShipmentStatus.IN_TRANSIT.equals(current) && ShipmentStatus.DELIVERED.equals(next));
    }
}
