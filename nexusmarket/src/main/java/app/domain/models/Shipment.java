package app.domain.models;

import java.time.LocalDateTime;

import app.domain.valueObjects.ShipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Shipment {
    private Order order;
    private Warehouse originWarehouse;
    private User disapatchManager;
    private ShipmentStatus shipmentStatus;
    private LocalDateTime dispatchDate;
}
