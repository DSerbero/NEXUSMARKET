package app.domain.models;

import java.time.LocalDateTime;

import app.domain.valueObjects.MovementType;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Inventory {
    private PhysicalProduct product;
    private Warehouse warehouse;
    private Integer availableQuantity;
    private MovementType movementType;
    private LocalDateTime movementDate;
}
