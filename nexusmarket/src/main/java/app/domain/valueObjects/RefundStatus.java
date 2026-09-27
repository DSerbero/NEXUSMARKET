package app.domain.valueObjects;


public final class RefundStatus extends DomainCatalog {
    
    public static final RefundStatus PENDING = new RefundStatus(
        "PENDING", "Pending", "The refund has been requested but not yet processed.");
    public static final RefundStatus PROCESSED = new RefundStatus(
        "PROCESSED", "Processed", "The funds have been returned to the buyer.");
    public static final RefundStatus REJECTED = new RefundStatus(
        "REJECTED", "Rejected", "The refund request was rejected.");
    
    private RefundStatus(String code, String name, String description) {
        super(code, name, description);
    }
}
