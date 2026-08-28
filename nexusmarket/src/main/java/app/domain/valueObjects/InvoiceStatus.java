package app.domain.valueObjects;

public final class InvoiceStatus extends DomainCatalog {
    
    public static final InvoiceStatus ENABLED = new InvoiceStatus(
        "ISSUED", "Issued", "The invoice has been generated.");
    public static final InvoiceStatus PAID = new InvoiceStatus(
        "PAID", "Paid", "The invoice amount has been collected.");
    public static final InvoiceStatus VOIDED = new InvoiceStatus(
        "VOIDED", "Voided", "The invoice has been canceled.");
    
    private InvoiceStatus(String code, String name, String description) {
        super(code, name, description);
    }
}

