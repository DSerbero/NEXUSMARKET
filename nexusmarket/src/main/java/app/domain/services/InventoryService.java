package app.domain.services;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.services.ports.InventoryRepositoryPort;
import app.domain.services.ports.WarehouseRepositoryPort;
import app.domain.valueObjects.MovementType;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(InventoryRepositoryPort.class)
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepositoryPort inventoryRepository;
    private final WarehouseRepositoryPort warehouseRepository;

    public Inventory registerInitialStock(User requestingUser, Inventory inventory, int quantity) {
        DomainValidations.active(requestingUser);
        validateInventory(inventory);
        if (quantity < 0) {
            throw new app.domain.services.exceptions.DomainValidationException("quantity cannot be negative");
        }
        inventory.setAvailableQuantity(quantity);
        inventory.setMovementType(MovementType.STOCK_IN);
        inventory.setMovementDate(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }

    public Inventory reserveStock(User requestingUser, Inventory inventory, int quantity) {
        DomainValidations.active(requestingUser);
        Inventory current = authoritative(inventory);
        validatePositive(quantity);
        if (current.getAvailableQuantity() == null || current.getAvailableQuantity() < quantity) {
            throw new app.domain.services.exceptions.DomainValidationException("Insufficient stock");
        }
        current.setAvailableQuantity(current.getAvailableQuantity() - quantity);
        current.setMovementType(MovementType.RESERVATION);
        current.setMovementDate(LocalDateTime.now());
        return inventoryRepository.update(current);
    }

    public Inventory releaseReservedStock(User requestingUser, Inventory inventory, int quantity) {
        DomainValidations.active(requestingUser);
        Inventory current = authoritative(inventory);
        validatePositive(quantity);
        current.setAvailableQuantity((current.getAvailableQuantity() == null ? 0 : current.getAvailableQuantity()) + quantity);
        current.setMovementType(MovementType.ADJUSTMENT);
        current.setMovementDate(LocalDateTime.now());
        return inventoryRepository.update(current);
    }

    public Inventory registerStockMovement(User requestingUser, Inventory inventory,
            MovementType movementType, int quantity) {
        DomainValidations.active(requestingUser);
        Inventory current = authoritative(inventory);
        validatePositive(quantity);
        if (MovementType.STOCK_IN.equals(movementType) || MovementType.RETURN_INBOUND.equals(movementType)) {
            current.setAvailableQuantity(value(current) + quantity);
        } else if (MovementType.SALE_OUTBOUND.equals(movementType)) {
            if (value(current) < quantity) {
                throw new app.domain.services.exceptions.DomainValidationException("Insufficient stock");
            }
            current.setAvailableQuantity(value(current) - quantity);
        } else {
            throw new app.domain.services.exceptions.DomainValidationException("Unsupported movement for this operation");
        }
        current.setMovementType(DomainValidations.required(movementType, "movementType"));
        current.setMovementDate(LocalDateTime.now());
        return inventoryRepository.update(current);
    }

    private Inventory authoritative(Inventory inventory) {
        return inventoryRepository.findByProductAndWarehouse(inventory).orElseThrow();
    }

    private void validateInventory(Inventory inventory) {
        DomainValidations.required(inventory, "inventory");
        DomainValidations.required(inventory.getProduct(), "product");
        DomainValidations.required(inventory.getWarehouse(), "warehouse");
        warehouseRepository.findByIdentifier(inventory.getWarehouse()).orElseThrow();
    }

    private int value(Inventory inventory) {
        return inventory.getAvailableQuantity() == null ? 0 : inventory.getAvailableQuantity();
    }

    private void validatePositive(int quantity) {
        if (quantity <= 0) {
            throw new app.domain.services.exceptions.DomainValidationException("quantity must be greater than zero");
        }
    }
}
