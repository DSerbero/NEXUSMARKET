package app.domain.services.inventory;

import app.domain.services.DomainValidations;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.exceptions.DomainValidationException;
import app.domain.valueobjects.MovementType;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.WarehouseOwnerType;
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
        authorizeMutation(requestingUser, inventory, false);
        if (quantity < 0) {
            throw new DomainValidationException("quantity cannot be negative");
        }
        if (inventoryRepository.findByProductAndWarehouse(inventory).isPresent()) {
            throw new DomainValidationException("Inventory already exists for product and warehouse");
        }
        inventory.setAvailableQuantity(quantity);
        inventory.setMovementType(MovementType.STOCK_IN);
        inventory.setMovementDate(LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }

    public Inventory reserveStock(User requestingUser, Inventory inventory, int quantity) {
        DomainValidations.active(requestingUser);
        Inventory current = authoritative(inventory);
        authorizeMutation(requestingUser, current, true);
        validatePositive(quantity);
        if (current.getAvailableQuantity() < quantity) {
            throw new DomainValidationException("Insufficient stock");
        }
        current.setAvailableQuantity(current.getAvailableQuantity() - quantity);
        current.setMovementType(MovementType.RESERVATION);
        current.setMovementDate(LocalDateTime.now());
        return inventoryRepository.update(current);
    }

    public Inventory releaseReservedStock(User requestingUser, Inventory inventory, int quantity) {
        DomainValidations.active(requestingUser);
        Inventory current = authoritative(inventory);
        authorizeMutation(requestingUser, current, true);
        validatePositive(quantity);
        current.setAvailableQuantity(add(current.getAvailableQuantity(), quantity));
        current.setMovementType(MovementType.ADJUSTMENT);
        current.setMovementDate(LocalDateTime.now());
        return inventoryRepository.update(current);
    }

    public Inventory registerStockMovement(User requestingUser, Inventory inventory,
            MovementType movementType, int quantity) {
        DomainValidations.active(requestingUser);
        Inventory current = authoritative(inventory);
        authorizeMutation(requestingUser, current, true);
        DomainValidations.required(movementType, "movementType");
        int available = current.getAvailableQuantity();
        if (MovementType.STOCK_IN.equals(movementType) || MovementType.RETURN_INBOUND.equals(movementType)) {
            validatePositive(quantity);
            current.setAvailableQuantity(add(available, quantity));
        } else if (MovementType.SALE_OUTBOUND.equals(movementType)) {
            validatePositive(quantity);
            if (available < quantity) {
                throw new DomainValidationException("Insufficient stock");
            }
            current.setAvailableQuantity(available - quantity);
        } else if (MovementType.ADJUSTMENT.equals(movementType)) {
            if (quantity == 0) {
                throw new DomainValidationException("Adjustment quantity cannot be zero");
            }
            long adjusted = (long) available + quantity;
            if (adjusted < 0 || adjusted > Integer.MAX_VALUE) {
                throw new DomainValidationException("Adjustment would result in invalid stock");
            }
            current.setAvailableQuantity((int) adjusted);
        } else {
            throw new DomainValidationException("Unsupported movement for this operation");
        }
        current.setMovementType(movementType);
        current.setMovementDate(LocalDateTime.now());
        return inventoryRepository.update(current);
    }

    private Inventory authoritative(Inventory inventory) {
        DomainValidations.required(inventory, "inventory");
        validateInventory(inventory);
        Inventory current = inventoryRepository.findByProductAndWarehouse(inventory)
                .orElseThrow(() -> new DomainValidationException("Inventory not found"));
        Integer quantity = DomainValidations.required(current.getAvailableQuantity(), "availableQuantity");
        if (quantity < 0) {
            throw new DomainValidationException("Inventory cannot contain negative stock");
        }
        return current;
    }

    private void validateInventory(Inventory inventory) {
        DomainValidations.required(inventory, "inventory");
        DomainValidations.required(inventory.getProduct(), "product");
        DomainValidations.required(inventory.getWarehouse(), "warehouse");
        warehouseRepository.findByIdentifier(inventory.getWarehouse())
                .orElseThrow(() -> new DomainValidationException("Warehouse not found"));
    }

    private int add(int available, int quantity) {
        long total = (long) available + quantity;
        if (total > Integer.MAX_VALUE) {
            throw new DomainValidationException("Stock quantity exceeds the supported range");
        }
        return (int) total;
    }

    private void validatePositive(int quantity) {
        if (quantity <= 0) {
            throw new DomainValidationException("quantity must be greater than zero");
        }
    }

    private void authorizeMutation(User requestingUser, Inventory inventory, boolean allowLogisticsOperator) {
        Warehouse warehouse = inventory.getWarehouse();
        if (UserRole.ADMIN.equals(requestingUser.getRole())) {
            return;
        }
        if (UserRole.SELLER.equals(requestingUser.getRole())
                && DomainValidations.sameIdentity(requestingUser, inventory.getProduct().getSeller())
                && DomainValidations.sameIdentity(requestingUser, warehouse.getOwner())) {
            return;
        }
        if (allowLogisticsOperator && UserRole.LOGISTICS_OPERATOR.equals(requestingUser.getRole())
                && WarehouseOwnerType.MARKETPLACE.equals(warehouse.getOwnerType())
                && DomainValidations.sameIdentity(requestingUser, warehouse.getResponsibleUser())) {
            return;
        }
        throw new DomainValidationException("User cannot modify inventory in this warehouse");
    }
}
