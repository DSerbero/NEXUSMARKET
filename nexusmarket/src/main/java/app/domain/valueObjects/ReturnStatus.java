package app.domain.valueObjects;


public final class ReturnStatus extends DomainCatalog {
    
    public static final ReturnStatus REQUESTED = new ReturnStatus(
        "REQUESTED", "Requested", "The return has been requested.");
    public static final ReturnStatus APPROVED = new ReturnStatus(
        "APPROVED", "Approved", "The return has been validated and accepted.");
    public static final ReturnStatus REJECTED = new ReturnStatus(
        "REJECTED", "Rejected", "The return request was rejected.");
    public static final ReturnStatus COMPLETED = new ReturnStatus(
        "COMPLETED", "Completed", "The returned product has been returned to inventory.");

    private ReturnStatus(String code, String name, String description) {
        super(code, name, description);
    }
}