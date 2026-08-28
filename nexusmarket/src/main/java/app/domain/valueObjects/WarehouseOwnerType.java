package app.domain.valueObjects;

public final class WarehouseOwnerType extends DomainCatalog {
    
    public static final WarehouseOwnerType MARKETPLACE = new WarehouseOwnerType(
        "MARKETPLACE", "Marketplace", "The warehouse belongs to and is operated directly by the Marketplace.");
    public static final WarehouseOwnerType SELLER = new WarehouseOwnerType(
        "SELLER", "Seller", "The warehouse belongs to and is operated by a seller.");
    
    private WarehouseOwnerType(String code, String name, String description) {
        super(code, name, description);
    }
}