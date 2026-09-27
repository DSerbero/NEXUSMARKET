package app.domain.services.ports;

import java.util.List;
import java.util.Optional;

import app.domain.models.Inventory;

public interface InventoryRepositoryPort {
    Inventory save(Inventory inventory);
    Optional<Inventory> findByProductAndWarehouse(Inventory inventory);
    List<Inventory> findByWarehouse(Inventory inventory);
    Inventory update(Inventory inventory);
}
