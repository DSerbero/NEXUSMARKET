package app.adapters.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import app.domain.models.Buyer;
import app.domain.models.Inventory;
import app.domain.models.Invoice;
import app.domain.models.Order;
import app.domain.models.PhysicalProduct;
import app.domain.models.Refund;
import app.domain.models.Return;
import app.domain.models.Seller;
import app.domain.models.Shipment;
import app.domain.models.User;
import app.domain.models.Warehouse;
import app.domain.ports.out.BuyerRepositoryPort;
import app.domain.ports.out.InventoryRepositoryPort;
import app.domain.ports.out.InvoiceRepositoryPort;
import app.domain.ports.out.OrderRepositoryPort;
import app.domain.ports.out.RefundRepositoryPort;
import app.domain.ports.out.ReturnRepositoryPort;
import app.domain.ports.out.SellerRepositoryPort;
import app.domain.ports.out.ShipmentRepositoryPort;
import app.domain.ports.out.WarehouseRepositoryPort;
import app.domain.valueobjects.BuyerStatus;
import app.domain.valueobjects.InvoiceStatus;
import app.domain.valueobjects.MovementType;
import app.domain.valueobjects.OrderStatus;
import app.domain.valueobjects.RefundStatus;
import app.domain.valueobjects.ReturnStatus;
import app.domain.valueobjects.ShipmentStatus;
import app.domain.valueobjects.UserRole;
import app.domain.valueobjects.UserStatus;
import app.domain.valueobjects.WarehouseOwnerType;

/**
 * Round-trip persistence tests for the JPA adapters of the Seller, Warehouse,
 * Inventory, Invoice, Shipment, Return and Refund aggregates, backed by H2 in
 * MySQL mode with ddl-auto=validate against the Flyway migrations.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:aggregates;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate"
})
class JpaAggregatesPersistenceIntegrationTest {

    @Autowired
    private SellerRepositoryPort sellerRepository;

    @Autowired
    private WarehouseRepositoryPort warehouseRepository;

    @Autowired
    private InventoryRepositoryPort inventoryRepository;

    @Autowired
    private InvoiceRepositoryPort invoiceRepository;

    @Autowired
    private ShipmentRepositoryPort shipmentRepository;

    @Autowired
    private ReturnRepositoryPort returnRepository;

    @Autowired
    private RefundRepositoryPort refundRepository;

    @Autowired
    private BuyerRepositoryPort buyerRepository;

    @Autowired
    private OrderRepositoryPort orderRepository;

    @Autowired
    private app.domain.ports.out.UserRepositoryPort userRepository;

    @Test
    void sellerRoundTripSharesUserIdentityAndSupportsEmailLookup() {
        Seller seller = new Seller();
        seller.setIdentifier(UUID.randomUUID().toString());
        seller.setFullName("Seller One");
        seller.setEmail(seller.getIdentifier() + "@example.test");
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        Seller saved = sellerRepository.save(seller);

        Seller byId = sellerRepository.findByIdentifier(sellerId(saved.getIdentifier())).orElseThrow();
        Seller byEmail = sellerRepository.findByEmail(sellerEmail(saved.getEmail())).orElseThrow();

        assertEquals("Seller One", byId.getFullName());
        assertEquals(UserRole.SELLER, byId.getRole());
        assertEquals(saved.getIdentifier(), byEmail.getIdentifier());
        assertTrue(sellerRepository.existsByEmail(sellerEmail(saved.getEmail())));
        assertFalse(sellerRepository.existsByEmail(sellerEmail("missing@example.test")));
    }

    @Test
    void warehouseRoundTripGeneratesIdentifierAndRehydratesOwner() {
        Seller owner = persistSeller();
        Warehouse warehouse = warehouseRepository.save(sellerWarehouse(owner));
        assertTrue(warehouse.getIdentifier() > 0);

        Warehouse found = warehouseRepository.findByIdentifier(warehouseId(warehouse.getIdentifier()))
                .orElseThrow();
        assertEquals("Main depot", found.getLocation());
        assertEquals(WarehouseOwnerType.SELLER, found.getOwnerType());
        assertNotNull(found.getOwner());
        assertEquals(owner.getIdentifier(), found.getOwner().getIdentifier());

        Warehouse ownerQuery = new Warehouse();
        ownerQuery.setOwner(owner);
        List<Warehouse> byOwner = warehouseRepository.findByOwner(ownerQuery);
        assertTrue(byOwner.stream().anyMatch(w -> w.getIdentifier() == warehouse.getIdentifier()));
    }

    @Test
    void inventoryRoundTripRehydratesWarehouseAndSupportsLookups() {
        Seller owner = persistSeller();
        Warehouse warehouse = warehouseRepository.save(sellerWarehouse(owner));
        PhysicalProduct product = physicalProduct();

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setAvailableQuantity(7);
        inventory.setMovementType(MovementType.STOCK_IN);
        inventory.setMovementDate(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);
        assertNotNull(saved.getIdentifier());

        Inventory found = inventoryRepository.findByProductAndWarehouse(inventoryQuery(product, warehouse))
                .orElseThrow();
        assertEquals(7, found.getAvailableQuantity());
        assertNotNull(found.getWarehouse());
        assertEquals(warehouse.getIdentifier(), found.getWarehouse().getIdentifier());

        found.setAvailableQuantity(3);
        found.setMovementType(MovementType.SALE_OUTBOUND);
        Inventory updated = inventoryRepository.update(found);
        assertEquals(3, updated.getAvailableQuantity());

        List<Inventory> byWarehouse = inventoryRepository.findByWarehouse(warehouseInventoryQuery(warehouse));
        assertTrue(byWarehouse.stream().anyMatch(i -> saved.getIdentifier().equals(i.getIdentifier())));
    }

    @Test
    void invoiceRoundTripIsUniquePerOrderAndSupportsStatusUpdate() {
        Order order = persistOrder(OrderStatus.PAID);

        Invoice invoice = new Invoice();
        invoice.setOrder(orderRef(order.getIdentifier()));
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setCurrencyCode("COP");
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setInvoiceStatus(InvoiceStatus.ISSUED);
        Invoice saved = invoiceRepository.save(invoice);
        assertNotNull(saved.getIdentifier());

        Invoice byOrder = invoiceRepository.findByOrder(orderInvoiceQuery(order.getIdentifier())).orElseThrow();
        assertEquals(new BigDecimal("100.00"), byOrder.getTotalAmount());
        assertEquals(InvoiceStatus.ISSUED, byOrder.getInvoiceStatus());

        byOrder.setInvoiceStatus(InvoiceStatus.PAID);
        Invoice updated = invoiceRepository.update(byOrder);
        assertEquals(InvoiceStatus.PAID, updated.getInvoiceStatus());
    }

    @Test
    void shipmentRoundTripRehydratesWarehouseAndOperatorAndSupportsLookups() {
        Seller owner = persistSeller();
        Warehouse warehouse = warehouseRepository.save(sellerWarehouse(owner));
        Order order = persistOrder(OrderStatus.DISPATCHED);
        User operator = operatorUser();

        Shipment shipment = new Shipment();
        shipment.setOrder(orderRef(order.getIdentifier()));
        shipment.setOriginWarehouse(warehouse);
        shipment.setOperator(operator);
        shipment.setShipmentStatus(ShipmentStatus.IN_PREPARATION);
        Shipment saved = shipmentRepository.save(shipment);
        assertNotNull(saved.getIdentifier());

        Shipment byOrder = shipmentRepository.findByOrder(orderShipmentQuery(order.getIdentifier())).orElseThrow();
        assertEquals(operator.getIdentifier(), byOrder.getOperator().getIdentifier());
        assertEquals(warehouse.getIdentifier(), byOrder.getOriginWarehouse().getIdentifier());

        byOrder.setShipmentStatus(ShipmentStatus.DISPATCHED);
        byOrder.setDispatchDate(LocalDateTime.now());
        Shipment updated = shipmentRepository.update(byOrder);
        assertEquals(ShipmentStatus.DISPATCHED, updated.getShipmentStatus());
        assertNotNull(updated.getDispatchDate());

        Shipment operatorQuery = new Shipment();
        operatorQuery.setOperator(operator);
        List<Shipment> byOperator = shipmentRepository.findByOperator(operatorQuery);
        assertTrue(byOperator.stream().anyMatch(s -> saved.getIdentifier().equals(s.getIdentifier())));
    }

    @Test
    void returnRoundTripTracksActiveStatusAndSupportsRefundLink() {
        Order order = persistOrder(OrderStatus.DELIVERED_FINALIZED);

        Return request = new Return();
        request.setOrder(orderRef(order.getIdentifier()));
        request.setReason("Damaged");
        request.setReturnStatus(ReturnStatus.REQUESTED);
        request.setRequestDate(LocalDateTime.now());
        Return saved = returnRepository.save(request);
        assertNotNull(saved.getIdentifier());

        assertTrue(returnRepository.findActiveByOrder(orderReturnQuery(order.getIdentifier())).isPresent());

        saved.setReturnStatus(ReturnStatus.COMPLETED);
        Return updated = returnRepository.update(saved);
        assertEquals(ReturnStatus.COMPLETED, updated.getReturnStatus());

        Refund refund = new Refund();
        refund.setRelatedReturn(returnRef(updated.getIdentifier()));
        refund.setRefundedAmount(new BigDecimal("50.00"));
        refund.setCurrencyCode("COP");
        refund.setRefundStatus(RefundStatus.PROCESSED);
        refund.setProcessingDate(LocalDateTime.now());
        refundRepository.save(refund);

        Refund foundRefund = refundRepository.findByReturn(returnRefundQuery(updated.getIdentifier())).orElseThrow();
        assertEquals(new BigDecimal("50.00"), foundRefund.getRefundedAmount());
        assertEquals(updated.getIdentifier(), foundRefund.getRelatedReturn().getIdentifier());
    }

    // ---- helpers ----

    private Seller persistSeller() {
        Seller seller = new Seller();
        seller.setIdentifier(UUID.randomUUID().toString());
        seller.setFullName("Seller");
        seller.setEmail(seller.getIdentifier() + "@example.test");
        seller.setRole(UserRole.SELLER);
        seller.setStatus(UserStatus.ACTIVE);
        return sellerRepository.save(seller);
    }

    private Warehouse sellerWarehouse(Seller owner) {
        Warehouse warehouse = new Warehouse();
        warehouse.setLocation("Main depot");
        warehouse.setOwnerType(WarehouseOwnerType.SELLER);
        warehouse.setOwner(owner);
        return warehouse;
    }

    private PhysicalProduct physicalProduct() {
        PhysicalProduct product = new PhysicalProduct();
        product.setIdentifier(UUID.randomUUID().toString());
        return product;
    }

    private User operatorUser() {
        User operator = new User();
        operator.setIdentifier(UUID.randomUUID().toString());
        operator.setFullName("Operator");
        operator.setEmail(operator.getIdentifier() + "@example.test");
        operator.setRole(UserRole.LOGISTICS_OPERATOR);
        operator.setStatus(UserStatus.ACTIVE);
        return userRepository.save(operator);
    }

    private Order persistOrder(OrderStatus status) {
        Buyer buyer = new Buyer();
        buyer.setIdentifier(UUID.randomUUID().toString());
        buyer.setFullName("Buyer");
        buyer.setEmail(buyer.getIdentifier() + "@example.test");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer.setBuyerStatus(BuyerStatus.ENABLED);
        buyer.setPrimaryAddress("Address");
        buyer = buyerRepository.save(buyer);

        Order order = new Order();
        order.setIdentifier(UUID.randomUUID().toString());
        order.setBuyer(buyer);
        order.setOrderStatus(status);
        order.setCreationDate(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private Seller sellerId(String identifier) {
        Seller seller = new Seller();
        seller.setIdentifier(identifier);
        return seller;
    }

    private Seller sellerEmail(String email) {
        Seller seller = new Seller();
        seller.setEmail(email);
        return seller;
    }

    private Warehouse warehouseId(long identifier) {
        Warehouse warehouse = new Warehouse();
        warehouse.setIdentifier(identifier);
        return warehouse;
    }

    private Inventory inventoryQuery(PhysicalProduct product, Warehouse warehouse) {
        Inventory query = new Inventory();
        query.setProduct(product);
        query.setWarehouse(warehouse);
        return query;
    }

    private Inventory warehouseInventoryQuery(Warehouse warehouse) {
        Inventory query = new Inventory();
        query.setWarehouse(warehouse);
        return query;
    }

    private Order orderRef(String identifier) {
        Order order = new Order();
        order.setIdentifier(identifier);
        return order;
    }

    private Invoice orderInvoiceQuery(String orderIdentifier) {
        Invoice invoice = new Invoice();
        invoice.setOrder(orderRef(orderIdentifier));
        return invoice;
    }

    private Shipment orderShipmentQuery(String orderIdentifier) {
        Shipment shipment = new Shipment();
        shipment.setOrder(orderRef(orderIdentifier));
        return shipment;
    }

    private Return orderReturnQuery(String orderIdentifier) {
        Return request = new Return();
        request.setOrder(orderRef(orderIdentifier));
        return request;
    }

    private Return returnRef(String returnIdentifier) {
        Return request = new Return();
        request.setIdentifier(returnIdentifier);
        return request;
    }

    private Refund returnRefundQuery(String returnIdentifier) {
        Refund refund = new Refund();
        refund.setRelatedReturn(returnRef(returnIdentifier));
        return refund;
    }
}

