package application.domain.models;

import java.time.LocalDateTime;

import application.domain.valueobjects.ShipmentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Shipment {
    private Order order;
    private Warehouse originWarehouse;
    private User operator;
    private ShipmentStatus shipmentStatus;
    private LocalDateTime dispatchDate;
}
