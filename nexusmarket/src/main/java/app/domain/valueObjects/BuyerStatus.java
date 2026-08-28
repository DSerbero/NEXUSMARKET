package app.domain.valueObjects;

public final class BuyerStatus extends DomainCatalog {
    
    public static final BuyerStatus ENABLED = new BuyerStatus(
        "ENABLED", "Enabled", "The buyer can place orders as normal.");
    public static final BuyerStatus SUSPENDED = new BuyerStatus(
        "SUSPENDED", "Suspended", "The buyer has a temporary restriction on purchasing.");
    
    private BuyerStatus(String code, String name, String description) {
        super(code, name, description);
    }
}
