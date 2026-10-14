package app.domain.models;

import java.time.LocalDateTime;

import app.domain.valueobjects.ShipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Shipment {
    private String identifier;
    private Order order;
    private Warehouse originWarehouse;
    private User operator;
    private ShipmentStatus shipmentStatus;
    private LocalDateTime dispatchDate;
}
