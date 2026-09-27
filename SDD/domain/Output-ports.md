# Output Ports

## Introduction

Este documento es el catálogo canónico de **Output Ports** del sistema NexusMarket, referenciado como `SDD/Domain/Output-ports.md` a lo largo de los 13 documentos de servicios.

Cada uno de los servicios de aplicación descritos en los documentos de subdominio (`Authorization Services`, `Seller Management Services`, etc.) declara qué Output Ports usa, pero el contrato completo de cada puerto se define **una sola vez, aquí** — los documentos de subdominio no deben redefinir un contrato ya canónico, solo referenciarlo.

Todos los puertos:

* Operan exclusivamente sobre Domain Models (nunca sobre identificadores primitivos como parámetro principal).
* Son implementados por adaptadores de persistencia — el dominio nunca depende de JPA, SQL, MongoDB, ni ningún detalle de infraestructura.
* Viven en el paquete de dominio/aplicación; sus implementaciones viven en la capa de adaptadores.

```text
Application Service
        |
        v
Output Port (interfaz, en el dominio)
        |
        v
Persistence Adapter (implementación, en infraestructura)
        |
        v
Database
```

---

# Port Ownership Matrix

| Output Port | Subdominio dueño | Contrato definido en |
|---|---|---|
| `UserRepositoryPort` | User and Authentication Management | Authorization Services, §UserRepositoryPort |
| `SellerRepositoryPort` | Seller Management | Seller Management Services, §14 |
| `BuyerRepositoryPort` | Buyer Management | Buyer Management Services, §13 |
| `WarehouseRepositoryPort` | Warehouse Management | Warehouse Management Services, §13 |
| `ProductRepositoryPort` | Product Catalog Management | Product Catalog Management Services, §13 |
| `InventoryRepositoryPort` | Inventory Management | Inventory Management Services, §13 |
| `CartRepositoryPort` | Cart Management | Cart Management Services, §13 |
| `OrderRepositoryPort` | Order Management | Order Management Services, §13 |
| `InvoiceRepositoryPort` | Invoicing Management | Invoicing Management Services, §13 |
| `ShipmentRepositoryPort` | Logistics Management | Logistics Management Services, §13 |
| `ReturnRepositoryPort` | Returns and Refunds Management | Returns and Refunds Management Services, §13 |
| `RefundRepositoryPort` | Returns and Refunds Management | Returns and Refunds Management Services, §13 |

**Regla de propiedad:** un Output Port pertenece exclusivamente al subdominio dueño de la entidad que persiste. Otro subdominio puede **usarlo** (por ejemplo, `Authorization Services` usa `ProductRepositoryPort` para validar propiedad), pero nunca debe redefinirlo ni crear una versión paralela.

---

# Cross-Subdomain Usage Matrix

Esta tabla resume qué subdominios, además del dueño, consumen cada puerto — construida a partir de las Service-to-Port Matrix de cada documento.

| Output Port | Usado también por |
|---|---|
| `UserRepositoryPort` | Authorization, Seller Management, Warehouse Management, Buyer Management (implícito vía User), Administrative Reporting |
| `SellerRepositoryPort` | Authorization, Product Catalog Management, Warehouse Management, Administrative Reporting |
| `BuyerRepositoryPort` | Authorization, Order Management, Administrative Reporting |
| `WarehouseRepositoryPort` | Authorization, Seller Management, Inventory Management, Logistics Management |
| `ProductRepositoryPort` | Authorization, Seller Management, Inventory Management, Cart Management, Administrative Reporting |
| `InventoryRepositoryPort` | Authorization, Administrative Reporting |
| `OrderRepositoryPort` | Authorization, Cart Management (indirecto vía CreateOrderUseCase), Invoicing Management, Logistics Management, Returns and Refunds Management, Administrative Reporting |
| `InvoiceRepositoryPort` | Returns and Refunds Management |
| `ReturnRepositoryPort` | Authorization, Administrative Reporting |

`CartRepositoryPort`, `ShipmentRepositoryPort` y `RefundRepositoryPort` son consumidos exclusivamente por su subdominio dueño hasta el momento.

---

# UserRepositoryPort

**Dueño:** User and Authentication Management.

```java
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findByEmail(User user);

    Optional<User> findById(User user);

    boolean existsByEmail(User user);

    void update(User user);
}
```

---

# SellerRepositoryPort

**Dueño:** Seller Management.

```java
public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findByIdentifier(Seller seller);

    Optional<Seller> findByEmail(Seller seller);

    boolean existsByEmail(Seller seller);

    Seller update(Seller seller);
}
```

---

# BuyerRepositoryPort

**Dueño:** Buyer Management.

```java
public interface BuyerRepositoryPort {

    Buyer save(Buyer buyer);

    Optional<Buyer> findByIdentifier(Buyer buyer);

    Optional<Buyer> findByEmail(Buyer buyer);

    boolean existsByEmail(Buyer buyer);

    Buyer update(Buyer buyer);
}
```

---

# WarehouseRepositoryPort

**Dueño:** Warehouse Management.

```java
public interface WarehouseRepositoryPort {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findByIdentifier(Warehouse warehouse);

    List<Warehouse> findByOwner(Warehouse warehouse);

    Warehouse update(Warehouse warehouse);
}
```

---

# ProductRepositoryPort

**Dueño:** Product Catalog Management.

```java
public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findByIdentifier(Product product);

    List<Product> findBySeller(Product product);

    Product update(Product product);
}
```

---

# InventoryRepositoryPort

**Dueño:** Inventory Management.

```java
public interface InventoryRepositoryPort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductAndWarehouse(Inventory inventory);

    List<Inventory> findByWarehouse(Inventory inventory);

    Inventory update(Inventory inventory);
}
```

---

# CartRepositoryPort

**Dueño:** Cart Management.

```java
public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findByBuyer(Cart cart);

    Cart update(Cart cart);
}
```

---

# OrderRepositoryPort

**Dueño:** Order Management.

```java
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findByIdentifier(Order order);

    List<Order> findByBuyer(Order order);

    Order update(Order order);
}
```

---

# InvoiceRepositoryPort

**Dueño:** Invoicing Management.

```java
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrder(Invoice invoice);

    Optional<Invoice> findByIdentifier(Invoice invoice);

    Invoice update(Invoice invoice);
}
```

---

# ShipmentRepositoryPort

**Dueño:** Logistics Management.

```java
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findByOrder(Shipment shipment);

    Optional<Shipment> findByIdentifier(Shipment shipment);

    List<Shipment> findByOperator(Shipment shipment);

    Shipment update(Shipment shipment);
}
```

---

# ReturnRepositoryPort

**Dueño:** Returns and Refunds Management.

```java
public interface ReturnRepositoryPort {

    Return save(Return returnRequest);

    Optional<Return> findByIdentifier(Return returnRequest);

    Optional<Return> findActiveByOrder(Return returnRequest);

    Return update(Return returnRequest);
}
```

---

# RefundRepositoryPort

**Dueño:** Returns and Refunds Management.

```java
public interface RefundRepositoryPort {

    Refund save(Refund refund);

    Optional<Refund> findByReturn(Refund refund);

    Optional<Refund> findByIdentifier(Refund refund);
}
```

---

# Naming Consistency Note

A diferencia del ejemplo bancario de referencia (que tenía un alias histórico, `AuditRepositoryPort` -> `AuditLogRepositoryPort`), NexusMarket no define actualmente un Domain Model equivalente a `AuditLog` (ver nota en Seller Management Services, §1), por lo que no existe un puerto de auditoría que reconciliar aquí. Si en el futuro se agrega trazabilidad histórica al dominio, este documento debe extenderse con el puerto correspondiente en ese momento — no antes.

---

# Pending Cross-Subdomain Input Ports (Not Output Ports)

Para evitar confusión: los siguientes son **Input Ports** de otros subdominios, invocados por coordinación entre servicios (no son Output Ports y no pertenecen a este documento, pero se listan aquí para referencia cruzada, ya que aparecen en las Service-to-Port Matrix de varios documentos bajo la columna "Cross-subdomain"):

```text
CreateOrderUseCase          (Order Management)      <- invocado por Cart Management
ReserveStockUseCase         (Inventory Management)  <- invocado por Order Management
ReleaseReservedStockUseCase (Inventory Management)  <- invocado por Order Management
RegisterStockMovementUseCase(Inventory Management)  <- invocado por Returns and Refunds Management
IssueInvoiceUseCase         (Invoicing Management)  <- invocado por Order Management
RegisterShipmentUseCase     (Logistics Management)  <- invocado por Order Management
FinalizeOrderUseCase        (Order Management)      <- invocado por Logistics Management
```

El catálogo completo de Input Ports por subdominio vive en cada documento de servicios respectivo (sección "Input Ports" de cada uno), no aquí.