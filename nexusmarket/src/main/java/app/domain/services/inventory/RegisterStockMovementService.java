package app.domain.services.inventory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import app.domain.models.Inventory;
import app.domain.models.User;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.valueobjects.MovementType;
import lombok.RequiredArgsConstructor;

/**
 * Register a Stock Movement: generic movement (STOCK_IN, RETURN_INBOUND,
 * SALE_OUTBOUND, ADJUSTMENT) applied to the authoritative inventory row.
 */
@Service
@ConditionalOnBean(InventoryRepositoryPort.class)
@RequiredArgsConstructor
public class RegisterStockMovementService {

    private final InventorySupport inventorySupport;
    private final InventoryRepositoryPort inventoryRepository;

    public Inventory execute(User requestingUser, Inventory inventory,
            MovementType movementType, int quantity) {
        Inventory current = inventorySupport.authoritative(inventory);
        inventorySupport.authorizeMutation(requestingUser, current, true);
        app.domain.services.DomainValidations.required(movementType, "movementType");
        int available = current.getAvailableQuantity();
        if (MovementType.STOCK_IN.equals(movementType) || MovementType.RETURN_INBOUND.equals(movementType)) {
            inventorySupport.validatePositive(quantity);
            current.setAvailableQuantity(inventorySupport.add(available, quantity));
        } else if (MovementType.SALE_OUTBOUND.equals(movementType)) {
            inventorySupport.validatePositive(quantity);
            if (available < quantity) {
                throw new app.domain.exceptions.DomainValidationException("Insufficient stock");
            }
            current.setAvailableQuantity(available - quantity);
        } else if (MovementType.ADJUSTMENT.equals(movementType)) {
            if (quantity == 0) {
                throw new app.domain.exceptions.DomainValidationException("Adjustment quantity cannot be zero");
            }
            long adjusted = (long) available + quantity;
            if (adjusted < 0 || adjusted > Integer.MAX_VALUE) {
                throw new app.domain.exceptions.DomainValidationException(
                        "Adjustment would result in invalid stock");
            }
            current.setAvailableQuantity((int) adjusted);
        } else {
            throw new app.domain.exceptions.DomainValidationException("Unsupported movement for this operation");
        }
        current.setMovementType(movementType);
        current.setMovementDate(java.time.LocalDateTime.now());
        return inventoryRepository.update(current);
    }
}