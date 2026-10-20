package app.domain.services.inventory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.valueobjects.MovementType;
import lombok.RequiredArgsConstructor;

/**
 * Release Reserved Stock: adds back quantity to available (e.g. cancelled
 * reservation). Type ADJUSTMENT.
 */
@Service
@ConditionalOnBean(InventoryRepositoryPort.class)
@RequiredArgsConstructor
public class ReleaseReservedStockService {

    private final InventorySupport inventorySupport;
    private final InventoryRepositoryPort inventoryRepository;

    public Inventory execute(User requestingUser, Inventory inventory, int quantity) {
        Inventory current = inventorySupport.authoritative(inventory);
        inventorySupport.authorizeMutation(requestingUser, current, true);
        inventorySupport.validatePositive(quantity);
        current.setAvailableQuantity(inventorySupport.add(current.getAvailableQuantity(), quantity));
        current.setMovementType(MovementType.ADJUSTMENT);
        current.setMovementDate(java.time.LocalDateTime.now());
        return inventoryRepository.update(current);
    }
}