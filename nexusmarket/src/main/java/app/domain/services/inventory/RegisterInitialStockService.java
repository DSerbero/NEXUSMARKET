package app.domain.services.inventory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.valueobjects.MovementType;
import lombok.RequiredArgsConstructor;

/**
 * Register Initial Stock: the first stock record for a product in a warehouse,
 * of type STOCK_IN. Rejects when stock is already registered for the pair.
 */
@Service
@ConditionalOnBean(InventoryRepositoryPort.class)
@RequiredArgsConstructor
public class RegisterInitialStockService {

    private final InventorySupport inventorySupport;
    private final InventoryRepositoryPort inventoryRepository;

    public Inventory execute(User requestingUser, Inventory inventory, int quantity) {
        inventorySupport.validateInventory(inventory);
        inventorySupport.authorizeMutation(requestingUser, inventory, false);
        if (quantity < 0) {
            throw new app.domain.exceptions.DomainValidationException("quantity cannot be negative");
        }
        if (inventoryRepository.findByProductAndWarehouse(inventory).isPresent()) {
            throw new app.domain.exceptions.DomainValidationException(
                    "Inventory already exists for product and warehouse");
        }
        inventory.setAvailableQuantity(quantity);
        inventory.setMovementType(MovementType.STOCK_IN);
        inventory.setMovementDate(java.time.LocalDateTime.now());
        return inventoryRepository.save(inventory);
    }
}
