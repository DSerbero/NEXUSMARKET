package app.domain.valueObjects;

public final class ShipmentStatus extends DomainCatalog {
    
    public static final ShipmentStatus IN_PREPARATION = new ShipmentStatus(
        "IN_PREPARATION", "In Preparation", "The order is being packed in the warehouse.");
    public static final ShipmentStatus DISPATCHED = new ShipmentStatus(
        "DISPATCHED", "Dispatched", "The shipment has left the warehouse.");
    public static final ShipmentStatus IN_TRANSIT = new ShipmentStatus(
        "IN_TRANSIT", "In Transit", "The shipment is in transit.");
    public static final ShipmentStatus DELIVERED = new ShipmentStatus(
        "DELIVERED", "Delivered", "The shipment has arrived at the buyer's address.");

    private ShipmentStatus(String code, String name, String description) {
        super(code, name, description);
    }
}