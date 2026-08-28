package app.domain.valueObjects;


public final class UserRole extends DomainCatalog {
    
    public static final UserRole BUYER = new UserRole(
        "BUYER", "Buyer", "Purchases products listed on the Marketplace.");
    public static final UserRole SELLER = new UserRole(
        "SELLER", "Seller", "Registers and manages products; only added by an Admin.");
    public static final UserRole LOGISTICS_OPERATOR = new UserRole(
        "LOGISTICS_OPERATOR", "Logistics Operator", "Logistics Operator. Manages the physical operation of warehouses and shipping.");
    public static final UserRole ADMIN = new UserRole(
        "ADMIN", "Admin", "Manages sellers and warehouses.");
    public static final UserRole SUPERVISOR = new UserRole(
        "SUPERVISOR", "Supervisor", "Read-only profile for viewing and monitoring.");

    private UserRole(String code, String name, String description) {
        super(code, name, description);
    }
}
