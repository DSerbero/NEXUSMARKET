package app.domain.services.inventory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.exceptions.DomainValidationException;
import app.domain.services.DomainValidations;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.WarehouseOwnerType;
import lombok.RequiredArgsConstructor;

/**
 * Read/write support for inventory: authoritative loading, stock-invariant
 * validation and warehouse mutation authorization. Each operation service in
 * this package reuses it so the rules live in one place.
 */
@Component
@RequiredArgsConstructor
class InventorySupport {

    private final InventoryRepositoryPort inventoryRepository;
    private final WarehouseRepositoryPort warehouseRepository;

    Inventory authoritative(Inventory inventory) {
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

    void validateInventory(Inventory inventory) {
        DomainValidations.required(inventory, "inventory");
        DomainValidations.required(inventory.getProduct(), "product");
        DomainValidations.required(inventory.getWarehouse(), "warehouse");
        warehouseRepository.findByIdentifier(inventory.getWarehouse())
                .orElseThrow(() -> new DomainValidationException("Warehouse not found"));
    }

    Integer add(int available, int quantity) {
        long total = (long) available + quantity;
        if (total > Integer.MAX_VALUE) {
            throw new DomainValidationException("Stock quantity exceeds the supported range");
        }
        return (int) total;
    }

    void validatePositive(int quantity) {
        if (quantity <= 0) {
            throw new DomainValidationException("quantity must be greater than zero");
        }
    }

    void authorizeMutation(User requestingUser, Inventory inventory, boolean allowLogisticsOperator) {
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