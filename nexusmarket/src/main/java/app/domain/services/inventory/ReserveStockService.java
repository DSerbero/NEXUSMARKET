package app.domain.services.inventory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.valueobjects.MovementType;
import lombok.RequiredArgsConstructor;

/**
 * Reserve Stock: atomically reserve quantity, decreasing available. Guards
 * against insufficient stock.
 */
@Service
@ConditionalOnBean(InventoryRepositoryPort.class)
@RequiredArgsConstructor
public class ReserveStockService {

    private final InventorySupport inventorySupport;
    private final InventoryRepositoryPort inventoryRepository;

    public Inventory execute(User requestingUser, Inventory inventory, int quantity) {
        Inventory current = inventorySupport.authoritative(inventory);
        inventorySupport.authorizeMutation(requestingUser, current, true);
        inventorySupport.validatePositive(quantity);
        if (current.getAvailableQuantity() < quantity) {
            throw new app.domain.exceptions.DomainValidationException("Insufficient stock");
        }
        current.setAvailableQuantity(current.getAvailableQuantity() - quantity);
        current.setMovementType(MovementType.RESERVATION);
        current.setMovementDate(java.time.LocalDateTime.now());
        return inventoryRepository.update(current);
    }
}