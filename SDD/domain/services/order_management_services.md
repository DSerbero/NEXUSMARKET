# Order Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Order Management** del sistema NexusMarket.

El subdominio Order Management es responsable del compromiso comercial formal entre un buyer y el Marketplace — la entidad central de todo el sistema.

Las principales capacidades de negocio son:

* Create Order.
* Consult Order.
* Confirm Order Payment.
* Dispatch Order.
* Finalize Order.

El pedido está representado por el Domain Model `Order`.

```text
Order
  |
  +-- buyer : Buyer
  +-- items : List<Product>
  +-- orderStatus : OrderStatus
  +-- creationDate : LocalDateTime
  +-- completionDate : LocalDateTime
```

Un `Order` avanza a través de un ciclo de vida definido:

```text
Pending Payment -> Paid -> Dispatched -> Delivered/Finalized
```

Un `Order` finalizado no puede modificarse bajo ninguna circunstancia — es la regla de negocio más estricta de este subdominio.

> **Nota de validación (resuelta):** `OrderStatus.CART` fue eliminado del catálogo en `Domain Value Objects`. La fase de selección provisional está representada exclusivamente por la entidad `Cart` (subdominio Cart Management); un `Order` no existe como objeto hasta que `Confirm Cart` lo crea, con estado inicial `PENDING_PAYMENT`.

---

# 2. Domain Model Context

## 2.1 Order

```text
Order
  |
  +-- buyer : Buyer
  +-- items : List<Product>
  +-- orderStatus : OrderStatus
  +-- creationDate : LocalDateTime
  +-- completionDate : LocalDateTime
```

## 2.2 Order Status Lifecycle

```text
PENDING_PAYMENT
      |
      v
    PAID
      |
      v
 DISPATCHED
      |
      v
DELIVERED_FINALIZED
```

Es un ciclo estrictamente secuencial — no existen transiciones hacia atrás ni saltos de etapa. `DELIVERED_FINALIZED` es un estado terminal.

## 2.3 Order and Cart Relationship

`Order` se crea a partir de un `Cart` confirmado (ver Cart Management Services, §17). `Order.items` se copia del `Cart.items` validado en el momento de la creación — no es una referencia viva al carrito, que ya fue vaciado tras la confirmación.

## 2.4 Order and Other Subdomains

```text
Order
  |
  ├── origina  --> Invoice     (Invoicing Management, al confirmar el pago)
  ├── origina  --> Shipment    (Logistics Management, al despachar)
  └── origina  --> Return      (Returns and Refunds Management, tras la entrega)
```

Order Management coordina con estos subdominios a través de sus Input Ports respectivos — nunca accede directamente a sus Output Ports (mismo principio ya aplicado entre Cart Management e Inventory Management).

---

# 3. Buyer, LogisticsOperator, and Order Relationship

```text
Requesting User
 |
 ├── Buyer               -> solo puede operar sobre Order.buyer == requestingUser
 ├── LogisticsOperator    -> solo puede avanzar el estado de despacho de Orders cuyo Shipment
 │                            esté asociado a su Warehouse asignada
 └── Admin/Supervisor     -> Admin puede operar según reglas específicas; Supervisor solo lectura
```

Un `Buyer` puede consultar y, en las etapas aplicables, cancelar su propio `Order` — pero nunca puede ejecutar `Dispatch Order` ni `Finalize Order`, que son operaciones del lado logístico/administrativo.

---

# 4. Service Design Principle

Cada servicio de Order Management representa una operación de negocio cohesiva. Dado que `Order` es la entidad central del sistema, sus servicios coordinan (sin fusionar responsabilidades) con Inventory, Invoicing y Logistics.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative Order
                |
                v
Validate requesting User
                |
                v
Validate Order status (transición permitida)
                |
                v
Coordinate with related subdomains (Inventory, Invoicing, Logistics)
                |
                v
Execute Domain behavior
                |
                v
Persist through Output Port
                |
                v
Return result
```

---

# 6. Input Contract

Incorrecto:

```java
dispatchOrder(String orderId);
```

Preferido:

```java
dispatchOrder(
    User requestingUser,
    Order order
);
```

---

# 7. Authoritative State

```text
Input Order
       |
       v
OrderRepositoryPort
       |
       v
Authoritative Order
       |
       v
Business Validation
```

El servicio nunca debe confiar en `orderStatus` suministrado por el caller para decidir si una transición es válida — la inmutabilidad de un `Order` finalizado depende enteramente de esta validación.

---

# 8. External Information

* `BuyerRepositoryPort` — para validar `buyer.buyerStatus`.
* `ReserveStockUseCase` / `ReleaseReservedStockUseCase` (Inventory Management) — para la reserva de stock al crear el pedido.
* `IssueInvoiceUseCase` (Invoicing Management) — al confirmar el pago.
* `RegisterShipmentUseCase` (Logistics Management) — al despachar.

Los servicios de aplicación nunca deben acceder directamente a la base de datos ni a los Output Ports de otros subdominios.

---

# 9. Order Validation

* `buyer` existe y `buyer.buyerStatus == ENABLED`.
* `items` no está vacío.
* Transición de `orderStatus` válida según el ciclo de vida (§2.2).
* Un `Order` con `orderStatus == DELIVERED_FINALIZED` es inmutable: ninguna operación de este subdominio puede modificarlo.

---

# 10. Domain Behavior

Preferido:

```java
order.confirmPayment();
order.dispatch();
order.finalize();
```

Evitar:

```java
order.setOrderStatus(...);
```

El Domain Model debe proteger la secuencialidad estricta del ciclo de vida — por ejemplo, `order.dispatch()` debe lanzar una excepción de dominio si `orderStatus != PAID`.

---

# 11. Input Ports

```text
CreateOrderUseCase
ConsultOrderUseCase
ConfirmOrderPaymentUseCase
DispatchOrderUseCase
FinalizeOrderUseCase
```

---

# 12. Output Ports

```text
OrderRepositoryPort
BuyerRepositoryPort
```

Adicionalmente, Order Management invoca los siguientes Input Ports de otros subdominios (no sus Output Ports directamente):

```text
ReserveStockUseCase            (Inventory Management)
ReleaseReservedStockUseCase    (Inventory Management)
IssueInvoiceUseCase            (Invoicing Management)
RegisterShipmentUseCase        (Logistics Management)
```

## Service-to-Port Matrix

| Service | OrderRepositoryPort | BuyerRepositoryPort | Cross-subdomain Input Ports |
|---|---:|---:|---:|
| Create Order | ✓ (save) | ✓ | `ReserveStockUseCase` |
| Consult Order | ✓ (read) | | |
| Confirm Order Payment | ✓ (update) | | `IssueInvoiceUseCase` |
| Dispatch Order | ✓ (update) | | `RegisterShipmentUseCase` |
| Finalize Order | ✓ (update) | | |

---

# 13. OrderRepositoryPort

```java
public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findByIdentifier(Order order);

    List<Order> findByBuyer(Order order);

    Order update(Order order);
}
```

---

# 14. Create Order

## 14.1 Purpose

Crea un `Order` formal a partir del `Cart` confirmado de un buyer y establece su estado inicial de pago pendiente.

Este servicio es invocado por `Cart Management` (`Confirm Cart`), no directamente por un `Buyer` a través de un endpoint separado (ver Cart Management Services, §17.3).

## 14.2 Input

```text
Cart (validatedCart)
```

## 14.3 Validations

* `Cart.items` no está vacío (ya validado por el caller, pero se revalida por seguridad de la operación).
* Cada `Product` en `Cart.items` sigue `PUBLISHED`.

## 14.4 Domain Creation and Inventory Coordination

```text
Cart
      |
      v
Validate
      |
      v
Create Order (orderStatus = PENDING_PAYMENT, items = Cart.items, buyer = Cart.buyer)
      |
      v
Para cada Product físico en items:
      |
      v
ReserveStockUseCase.execute(product, warehouse, quantity)
      |
      ├── Reserva exitosa para todos los items -> continuar
      |
      └── Falla en algún item -> revertir reservas ya realizadas (compensación)
              y rechazar la creación del Order
      |
      v
OrderRepositoryPort.save(order)
```

La reserva de inventario y la persistencia del `Order` deben ser consistentes: si falla la reserva de cualquier producto físico, no debe quedar un `Order` creado sin su stock correspondiente reservado.

---

# 15. Consult Order

## 15.1 Purpose

Recupera un `Order` de acuerdo con los permisos del usuario solicitante.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Order.
3. Validate Order existence.
4. Validate authorization: Buyer dueño, LogisticsOperator asignado al Shipment relacionado, Admin, o Supervisor.
5. Return Order.
```

---

# 16. Confirm Order Payment

## 16.1 Purpose

Confirma el pago de un `Order` pendiente y lo avanza al estado pagado, iniciando el proceso de alistamiento.

## 16.2 Required Validations

* `Order` existe.
* `Order.orderStatus == PENDING_PAYMENT` — cualquier otro estado actual rechaza la operación.
* Confirmación de pago válida (el mecanismo de pago en sí está fuera del alcance de este subdominio — este servicio asume que la confirmación ya fue validada por el proceso de pago correspondiente).

## 16.3 Domain Behavior and Invoicing Coordination

```text
order.confirmPayment()
      |
      v
OrderRepositoryPort.update(order)
      |
      v
IssueInvoiceUseCase.execute(order)
```

La emisión de la `Invoice` es una consecuencia directa de esta operación, pero es responsabilidad de `Invoicing Management`, no de `Order Management` — este servicio invoca su Input Port, sin construir la `Invoice` directamente.

---

# 17. Dispatch Order

## 17.1 Purpose

Marca un `Order` como despachado una vez que ha salido físicamente de la warehouse.

## 17.2 Required Validations

* `requestingUser.role == LOGISTICS_OPERATOR` (asignado a la warehouse de origen) o `ADMIN`.
* `Order` existe.
* `Order.orderStatus == PAID` — no se puede despachar un pedido sin pago confirmado.

## 17.3 Domain Behavior and Logistics Coordination

```text
order.dispatch()
      |
      v
OrderRepositoryPort.update(order)
      |
      v
RegisterShipmentUseCase.execute(order, originWarehouse, operator)
```

Si el `Order` contiene únicamente productos digitales, este servicio no aplica — un `Order` compuesto solo por `DigitalProduct` avanza de `PAID` a `DELIVERED_FINALIZED` sin pasar por despacho físico (ver §17.4). Esta regla ya fue confirmada y no está sujeta a validación adicional.

## 17.4 Digital-Only Orders

Regla de negocio confirmada:

```text
Order (solo DigitalProduct)
      |
      v
PAID -> DELIVERED_FINALIZED  (directo, sin DISPATCHED)
```

Esta es una excepción explícita al ciclo de vida general (§2.2). Debe validarse revisando si todos los `items` del `Order` son `DigitalProduct` antes de decidir si el siguiente paso es `Dispatch Order` o directamente `Finalize Order`.

---

# 18. Finalize Order

## 18.1 Purpose

Marca un `Order` como entregado y finalizado tras la confirmación de entrega, dejándolo inmutable.

## 18.2 Required Validations

* `Order` existe.
* `Order.orderStatus == DISPATCHED` (para pedidos con productos físicos) o `Order.orderStatus == PAID` (para pedidos exclusivamente digitales, ver §17.4).
* Confirmación de entrega válida (recepción por el buyer, o entrega inmediata para productos digitales).

## 18.3 Domain Behavior

```java
order.finalize();
```

## 18.4 Immutability

Una vez `orderStatus == DELIVERED_FINALIZED`, ningún servicio de este subdominio puede modificar el `Order`. Cualquier incidencia posterior (producto dañado, arrepentimiento del comprador) se gestiona a través de `Returns and Refunds Management`, que opera sobre el `Order` finalizado sin alterarlo — crea un `Return` asociado, no reabre el pedido.

---

# 19. Exception Model

```text
OrderNotFoundException
EmptyOrderException
InvalidOrderStatusTransitionException
OrderAlreadyFinalizedException
UnauthorizedOrderOperationException
InventoryReservationFailedException
BuyerNotEligibleException
InvalidUserStatusException
```

---

# 20. Persistence Boundary

```text
OrderManagementService
        |
        v
OrderRepositoryPort
        |
        v
OrderPersistenceAdapter
        |
        v
Database
```

---

# 21. Controller Responsibilities

Los controllers no deben implementar:

* Reglas de transición de estado.
* Coordinación con Inventory/Invoicing/Logistics.
* Persistencia.

---

# 22. Validation Matrix

| Validation | Create | Consult | Confirm Payment | Dispatch | Finalize |
|---|---:|---:|---:|---:|---:|
| Requesting User | N/A (interno, vía Cart) | Yes | Yes | Yes | Yes |
| Order existence | N/A (se crea) | Yes | Yes | Yes | Yes |
| Status transition validity | N/A (estado inicial) | N/A | Yes (PENDING_PAYMENT) | Yes (PAID) | Yes (DISPATCHED o PAID si digital) |
| Inventory reservation | Yes | N/A | N/A | N/A | N/A |
| Invoice issuance | N/A | N/A | Yes | N/A | N/A |
| Shipment registration | N/A | N/A | N/A | Yes | N/A |
| Immutability check | N/A | N/A | Yes | Yes | Yes |

---

# 23. Business Rules Summary

## BR-001 — Order Lifecycle Is Strictly Sequential

`PENDING_PAYMENT -> PAID -> DISPATCHED -> DELIVERED_FINALIZED`, sin saltos ni retrocesos (con la excepción explícita de pedidos 100% digitales, BR-006).

## BR-002 — Order Is Created Only From a Confirmed Cart

`Create Order` no es un endpoint independiente invocado directamente por un Buyer.

## BR-003 — Finalized Orders Are Immutable

## BR-004 — Inventory Reservation Is Atomic With Order Creation

Si la reserva falla para cualquier producto físico, no debe crearse el `Order`.

## BR-005 — Invoicing and Logistics Are Delegated, Not Embedded

`Order Management` invoca los Input Ports de esos subdominios; no construye `Invoice` ni `Shipment` directamente.

## BR-006 — Digital-Only Orders Skip Dispatch

## BR-007 — Domain State Changes Through Domain Behavior

## BR-008 — Services Must Be Cohesive

---

# 24. Anti-Patterns

## 24.1 Direct Database Access

## 24.2 Primitive Application Contracts

## 24.3 Trusting Caller-Supplied Order Status

## 24.4 Bypassing Cart Confirmation

Inválido: un endpoint que permita crear un `Order` sin pasar por `Confirm Cart`.

## 24.5 Order Management Building Invoice/Shipment Directly

Inválido:

```text
ConfirmOrderPaymentService
     |
     +-- new Invoice(...)
     +-- InvoiceRepositoryPort.save(invoice)
```

Debe delegarse a `IssueInvoiceUseCase`.

## 24.6 Modifying a Finalized Order

Inválido: cualquier setter o método de dominio que permita alterar un `Order` con `orderStatus == DELIVERED_FINALIZED`.

---

# 25. Testing Requirements

## 25.1 Create Order Tests

* Creación válida con reserva de inventario exitosa para todos los items.
* Falla de reserva en un item revierte las reservas ya hechas y no crea el `Order`.
* Carrito vacío (rechazado).

## 25.2 Consult Order Tests

* Buyer consulta su propio order.
* Buyer intenta consultar order de otro buyer (rechazado).
* LogisticsOperator asignado consulta (permitido).

## 25.3 Confirm Order Payment Tests

* Confirmación válida desde `PENDING_PAYMENT`.
* Intento de confirmar un order ya `PAID` (rechazado).
* Emisión de `Invoice` verificada tras confirmación exitosa.

## 25.4 Dispatch Order Tests

* Despacho válido desde `PAID`.
* Intento de despachar sin pago confirmado (rechazado).
* Order 100% digital no pasa por este servicio.
* Registro de `Shipment` verificado tras despacho exitoso.

## 25.5 Finalize Order Tests

* Finalización válida desde `DISPATCHED`.
* Finalización válida desde `PAID` para order 100% digital.
* Intento de finalizar un order ya finalizado (rechazado).
* Intento de modificar un order finalizado por cualquier otro servicio (rechazado).

---

# 26. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models apropiados.
* No expone DTOs REST como contratos de aplicación.
* Recupera el `Order` autoritativo antes de validar transiciones.
* Coordina con Inventory, Invoicing y Logistics a través de sus Input Ports, nunca accediendo a sus Output Ports directamente.
* Garantiza la secuencialidad estricta del ciclo de vida y la inmutabilidad tras `DELIVERED_FINALIZED`.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 27. Final Service Catalog

```text
Order Management
|
+-- Create Order
|
+-- Consult Order
|
+-- Confirm Order Payment
|
+-- Dispatch Order
|
+-- Finalize Order
```

---

# 28. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando la secuencialidad estricta del ciclo de vida del Order, coordinando — sin fusionar responsabilidades — con Inventory, Invoicing y Logistics a través de sus Input Ports, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**