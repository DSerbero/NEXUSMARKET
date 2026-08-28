package app.domain.valueObjects;

public final class OrderStatus extends DomainCatalog {
    
    public static final OrderStatus CART = new OrderStatus(
        "CART", "Cart", "Provisional product selection.");
    public static final OrderStatus PENDING_PAYMENT = new OrderStatus(
        "PENDING_PAYMENT", "Pending Payment", "Awaiting financial confirmation.");
    public static final OrderStatus PAID = new OrderStatus(
        "PAID", "Paid", "Payment confirmed; order preparation begins.");
    public static final OrderStatus DISPATCHED = new OrderStatus(
        "DISPATCHED", "Dispatched", "The order has physically left the warehouse.");
    public static final OrderStatus DELIVERED_FINALIZED = new OrderStatus(
        "DELIVERED_FINALIZED", "Delivered / Finalized", "Delivery confirmed; order lifecycle complete.");

    private OrderStatus(String code, String name, String description) {
        super(code, name, description);
    }
}