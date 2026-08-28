package app.domain.valueObjects;


public final class UserStatus extends DomainCatalog {
    
    public static final UserStatus ACTIVE = new UserStatus(
        "ACTIVE", "Active", "The user can access and operate normally on the system.");
    public static final UserStatus BLOCKED = new UserStatus(
        "BLOCKED", "Blocked", "The user's access has been suspended.");
    public static final UserStatus INACTIVE = new UserStatus(
        "INACTIVE", "Inactive", "The user exists but is not currently operating on the platform.");
    
    private UserStatus(String code, String name, String description) {
        super(code, name, description);
    }
}
