package app.domain.services.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import app.domain.exceptions.DomainValidationException;
import app.domain.models.Inventory;
import app.domain.models.PhysicalProduct;
import app.domain.models.Seller;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.MovementType;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import app.domain.valueobjects.WarehouseOwnerType;

class InventoryServiceBusinessRulesTest {

    private final InventoryRepositoryPort inventoryRepository = mock(InventoryRepositoryPort.class);
    private final WarehouseRepositoryPort warehouseRepository = mock(WarehouseRepositoryPort.class);

    private RegisterInitialStockService register() {
        return new RegisterInitialStockService(inventorySupport(), inventoryRepository);
    }

    private ReserveStockService reserve() {
        return new ReserveStockService(inventorySupport(), inventoryRepository);
    }

    private ReleaseReservedStockService release() {
        return new ReleaseReservedStockService(inventorySupport(), inventoryRepository);
    }

    private RegisterStockMovementService movement() {
        return new RegisterStockMovementService(inventorySupport(), inventoryRepository);
    }

    private InventorySupport inventorySupport() {
        return new InventorySupport(inventoryRepository, warehouseRepository);
    }

    @Test
    void registerInitialStockCreatesStockIn() {
        Seller seller = user();
        Warehouse warehouse = warehouse(seller);
        Inventory inventory = stock(seller, warehouse, 10);
        when(inventoryRepository.findByProductAndWarehouse(any())).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenReturn(inventory);

        assertEquals(10, register().execute(seller, inventory, 10).getAvailableQuantity());
        assertEquals(MovementType.STOCK_IN, register().execute(seller, inventory, 10).getMovementType());
    }

    @Test
    void reserveStockDecreasesAvailable() {
        Seller operator = user();
        Warehouse warehouse = marketplace(operator);
        Inventory inventory = stock(operator, warehouse, 10);
        when(inventoryRepository.findByProductAndWarehouse(any())).thenReturn(Optional.of(inventory));
        when(inventoryRepository.update(any())).thenReturn(inventory);

        assertEquals(6, reserve().execute(operator, inventory, 4).getAvailableQuantity());
        assertEquals(MovementType.RESERVATION, reserve().execute(operator, inventory, 4).getMovementType());
    }

    @Test
    void movementStockInAddsAvailable() {
        Seller operator = user();
        Warehouse warehouse = marketplace(operator);
        Inventory inventory = stock(operator, warehouse, 10);
        when(inventoryRepository.findByProductAndWarehouse(any())).thenReturn(Optional.of(inventory));
        when(inventoryRepository.update(any())).thenReturn(inventory);

        assertEquals(15, movement().execute(operator, inventory, MovementType.STOCK_IN, 5).getAvailableQuantity());
    }

    @Test
    void movementSaleOutboundDeductsAvailable() {
        Seller operator = user();
        Warehouse warehouse = marketplace(operator);
        Inventory inventory = stock(operator, warehouse, 10);
        when(inventoryRepository.findByProductAndWarehouse(any())).thenReturn(Optional.of(inventory));
        when(inventoryRepository.update(any())).thenReturn(inventory);

        assertEquals(7, movement().execute(operator, inventory, MovementType.SALE_OUTBOUND, 3).getAvailableQuantity());
    }

    @Test
    void movementSaleOutboundRejectsInsufficientStock() {
        Seller operator = user();
        Warehouse warehouse = marketplace(operator);
        Inventory inventory = stock(operator, warehouse, 2);
        when(inventoryRepository.findByProductAndWarehouse(any())).thenReturn(Optional.of(inventory));

        assertThrows(DomainValidationException.class, () -> movement().execute(operator, inventory, MovementType.SALE_OUTBOUND, 5));
    }

    @Test
    void movementReleaseIncreasesAvailable() {
        Seller operator = user();
        Warehouse warehouse = marketplace(operator);
        Inventory inventory = stock(operator, warehouse, 10);
        when(inventoryRepository.findByProductAndWarehouse(any())).thenReturn(Optional.of(inventory));
        when(inventoryRepository.update(any())).thenReturn(inventory);

        assertEquals(17, release().execute(operator, inventory, 7).getAvailableQuantity());
    }

    private Seller user() {
        Seller u = new Seller();
        u.setIdentifier("user-1");
        u.setRole(UserRole.SELLER);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private Warehouse marketplace(User owner) {
        Warehouse w = new Warehouse();
        w.setIdentifier(1);
        w.setOwnerType(WarehouseOwnerType.MARKETPLACE);
        w.setOwner(owner); when(warehouseRepository.findByIdentifier(any())).thenReturn(Optional.of(w));
        w.setResponsibleUser(owner); when(warehouseRepository.findByIdentifier(any())).thenReturn(Optional.of(w));
        return w;
    }

    private Warehouse warehouse(User owner) {
        Warehouse w = new Warehouse();
        w.setIdentifier(1);
        w.setOwnerType(WarehouseOwnerType.SELLER);
        w.setOwner(owner); when(warehouseRepository.findByIdentifier(any())).thenReturn(Optional.of(w));
        return w;
    }

    private Inventory stock(Seller seller, Warehouse warehouse, int available) {
        Inventory inventory = new Inventory();
        PhysicalProduct product = new PhysicalProduct();
        product.setSeller(seller);
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setAvailableQuantity(available);
        return inventory;
    }
}



