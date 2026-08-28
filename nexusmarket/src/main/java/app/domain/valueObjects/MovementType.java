package app.domain.valueObjects;


public final class MovementType extends DomainCatalog {
    
    public static final MovementType STOCK_IN = new MovementType(
        "STOCK_IN", "Stock In", "Stock received into the warehouse.");
    public static final MovementType RESERVATION = new MovementType(
        "RESERVATION", "Reservation", "Stock reserved for a pending order.");
    public static final MovementType SALE_OUTBOUND = new MovementType(
        "SALE_OUTBOUND", "Sale Outbound", "Stock withdrawn due to a confirmed sale.");
    public static final MovementType ADJUSTMENT = new MovementType(
        "ADJUSTMENT", "Adjustment", "Manual adjustment of stock quantity.");
    public static final MovementType RETURN_INBOUND = new MovementType(
        "RETURN_INBOUND", "Return Inbound", "Stock returned after a return.");

    private MovementType(String code, String name, String description) {
        super(code, name, description);
    }
}
