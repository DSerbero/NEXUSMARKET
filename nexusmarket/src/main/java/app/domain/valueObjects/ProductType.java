package app.domain.valueObjects;

public final class ProductType extends DomainCatalog {
    
    public static final ProductType PHYSICAL = new ProductType(
        "PHYSICAL", "Physical", "Requires inventory control and shipping.");
    public static final ProductType DIGITAL = new ProductType(
        "DIGITAL", "Digital", "Delivered immediately upon payment confirmation.");
    
    private ProductType(String code, String name, String description) {
        super(code, name, description);
    }
}