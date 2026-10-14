package application.adapters.useCases.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import application.domain.exceptions.DomainValidationException;
import application.domain.models.Inventory;
import application.domain.models.PhysicalProduct;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.MovementType;
import application.domain.valueobjects.UserRole;
import application.domain.valueobjects.UserStatus;

class InventoryServiceBusinessRulesTest {

    @Test
    void adjustmentCannotMakeStockNegative() {
        InventoryRepositoryPort inventoryRepository = mock(InventoryRepositoryPort.class);
        WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);
        InventoryService service = new InventoryService(inventoryRepository, warehouseRepository);
        Warehouse warehouse = new Warehouse();
        Inventory inventory = inventory(warehouse, 5);
        User admin = activeAdmin();
        when(warehouseRepository.findByIdentifier(warehouse)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductAndWarehouse(inventory)).thenReturn(Optional.of(inventory));

        assertThrows(DomainValidationException.class,
                () -> service.registerStockMovement(admin, inventory, MovementType.ADJUSTMENT, -6));
    }

    @Test
    void initialStockRejectsNegativeQuantityBeforePersistence() {
        InventoryRepositoryPort inventoryRepository = mock(InventoryRepositoryPort.class);
        WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);
        InventoryService service = new InventoryService(inventoryRepository, warehouseRepository);
        Warehouse warehouse = new Warehouse();
        Inventory inventory = inventory(warehouse, null);
        when(warehouseRepository.findByIdentifier(warehouse)).thenReturn(Optional.of(warehouse));

        assertThrows(DomainValidationException.class,
                () -> service.registerInitialStock(activeAdmin(), inventory, -1));
        verifyNoInteractions(inventoryRepository);
    }

    private Inventory inventory(Warehouse warehouse, Integer quantity) {
        Inventory inventory = new Inventory();
        inventory.setProduct(new PhysicalProduct());
        inventory.setWarehouse(warehouse);
        inventory.setAvailableQuantity(quantity);
        return inventory;
    }

    private User activeAdmin() {
        User user = new User();
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}