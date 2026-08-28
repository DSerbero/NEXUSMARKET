package app.domain.valueObjects;


public final class ProductStatus extends DomainCatalog {
    
    public static final ProductStatus PUBLISHED = new ProductStatus(
        "PUBLISHED", "Published", "The product is visible in the public catalog.");
    public static final ProductStatus SUSPENDED = new ProductStatus(
        "SUSPENDED", "Suspended", "The product is temporarily hidden from the catalog.");
    public static final ProductStatus DISCONTINUED = new ProductStatus(
        "DISCONTINUED", "Discontinued", "The product is permanently withdrawn from sale.");
    
    private ProductStatus(String code, String name, String description) {
        super(code, name, description);
    }
}
