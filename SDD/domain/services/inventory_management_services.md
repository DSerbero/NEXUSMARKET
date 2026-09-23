# Inventory Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Inventory Management** del sistema NexusMarket.

El subdominio Inventory Management es responsable de controlar el stock distribuido de los productos físicos a través de las warehouses.

Las principales capacidades de negocio son:

* Register Initial Stock.
* Consult Inventory.
* Reserve Stock.
* Release Reserved Stock.
* Register Stock Movement.

El inventario está representado por el Domain Model `Inventory`.

```text
Inventory
  |
  +-- product : PhysicalProduct
  +-- warehouse : Warehouse
  +-- availableQuantity : Integer
  +-- movementType : MovementType
  +-- movementDate : LocalDateTime
```

**Regla de negocio central (Dominio 6 de la especificación):** el inventario debe estar vinculado obligatoriamente a un `PhysicalProduct` y una `Warehouse` específicos, y en ningún caso se permite stock negativo. Esta última condición es, junto con la trazabilidad de movimientos, la regla más crítica de todo el subdominio.

> Solo los `PhysicalProduct` tienen inventario — un `DigitalProduct` nunca debe tener un registro de `Inventory` asociado, ya que se entrega directamente sin pasar por una warehouse.

---

# 2. Domain Model Context

## 2.1 Inventory

```text
Inventory
  |
  +-- product : PhysicalProduct
  +-- warehouse : Warehouse
  +-- availableQuantity : Integer
  +-- movementType : MovementType
  +-- movementDate : LocalDateTime
```

## 2.2 MovementType

```text
STOCK_IN         -> ingreso de stock
RESERVATION      -> reserva para un Order pendiente
SALE_OUTBOUND    -> salida por venta confirmada
ADJUSTMENT       -> corrección manual de cantidad
RETURN_INBOUND   -> reingreso tras un Return
```

Cada operación de este subdominio corresponde a uno o más de estos tipos de movimiento — el `MovementType` no es un dato arbitrario que el caller elija libremente, sino una consecuencia directa de cuál servicio se está ejecutando.

---

# 3. Seller, LogisticsOperator, and Inventory Relationship

```text
Requesting User
 |
 v
Inventory.warehouse
 |
 ├── ownerType == SELLER       -> Warehouse.owner debe ser el Seller solicitante
 └── ownerType == MARKETPLACE  -> Warehouse.responsibleUser debe ser el LogisticsOperator solicitante
```

`ADMIN` puede operar sobre cualquier `Inventory`, sin restricción. Un `Buyer` nunca administra inventario — regla de negocio explícita de la especificación (Dominio 2: "un comprador nunca gestiona... datos de inventario").

---

# 4. Service Design Principle

Cada servicio de Inventory Management representa una operación de negocio cohesiva, responsable de validar todas las condiciones necesarias para ejecutarla correctamente — incluyendo, siempre, la regla de no-negatividad del stock.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative Inventory
                |
                v
Validate requesting User (Seller/Operator sobre su warehouse, o Admin)
                |
                v
Validate Product-Warehouse relationship
                |
                v
Validate quantity rules (nunca negativo)
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
reserveStock(
    String productId,
    String warehouseId,
    int quantity
);
```

Preferido:

```java
reserveStock(
    User requestingUser,
    Inventory inventory,
    int quantity
);
```

---

# 7. Authoritative State

```text
Input Inventory
       |
       v
InventoryRepositoryPort
       |
       v
Authoritative Inventory
       |
       v
Business Validation
```

Esta es, de todo el sistema, la validación de estado autoritativo más crítica: el servicio **nunca** debe confiar en `availableQuantity` suministrado por el caller para decidir si una reserva o salida es posible. Hacerlo permitiría condiciones de carrera que produzcan stock negativo — justo lo que la regla de negocio prohíbe explícitamente.

---

# 8. External Information

Cuando se requiere validar que el `warehouse` corresponde al seller u operador solicitante, el servicio debe usar `WarehouseRepositoryPort`. Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Inventory Validation

* `product` es un `PhysicalProduct` existente (nunca un `DigitalProduct`).
* `warehouse` existe.
* La combinación `product` + `warehouse` corresponde a un `Inventory` único (no deben crearse registros duplicados para el mismo par).
* `availableQuantity >= 0` en todo momento, antes y después de cualquier operación.

---

# 10. Domain Behavior

Preferido:

```java
inventory.registerStockIn(quantity);
inventory.reserve(quantity);
inventory.releaseReservation(quantity);
inventory.registerSaleOutbound(quantity);
inventory.registerAdjustment(quantity);
inventory.registerReturnInbound(quantity);
```

Evitar:

```java
inventory.setAvailableQuantity(
    inventory.getAvailableQuantity() - quantity
);
```

El Domain Model debe proteger su propio invariante central:

```text
availableQuantity >= 0
```

lanzando una excepción de dominio (`InsufficientStockException`) si una operación intenta violarlo, en vez de dejar que el servicio de aplicación calcule y asigne el valor manualmente.

---

# 11. Input Ports

```text
RegisterInitialStockUseCase
ConsultInventoryUseCase
ReserveStockUseCase
ReleaseReservedStockUseCase
RegisterStockMovementUseCase
```

---

# 12. Output Ports

```text
InventoryRepositoryPort
ProductRepositoryPort
WarehouseRepositoryPort
```

## Service-to-Port Matrix

| Service | InventoryRepositoryPort | ProductRepositoryPort | WarehouseRepositoryPort |
|---|---:|---:|---:|
| Register Initial Stock | ✓ (save) | ✓ (validar PhysicalProduct) | ✓ (validar warehouse) |
| Consult Inventory | ✓ (read) | | |
| Reserve Stock | ✓ (update) | | |
| Release Reserved Stock | ✓ (update) | | |
| Register Stock Movement | ✓ (update) | | |

---

# 13. InventoryRepositoryPort

```java
public interface InventoryRepositoryPort {

    Inventory save(Inventory inventory);

    Optional<Inventory> findByProductAndWarehouse(Inventory inventory);

    List<Inventory> findByWarehouse(Inventory inventory);

    Inventory update(Inventory inventory);
}
```

`findByProductAndWarehouse` es la operación central de este puerto — casi todos los servicios del subdominio la usan para recuperar el estado autoritativo antes de mutar la cantidad disponible.

---

# 14. Register Initial Stock

## 14.1 Purpose

Registra la cantidad inicial disponible de un `PhysicalProduct` en una `Warehouse` específica.

## 14.2 Input

```text
User (requestingUser)
PhysicalProduct
Warehouse
Integer (initialQuantity)
```

## 14.3 Validations

* `requestingUser` es el `Seller` dueño del producto, o `ADMIN`.
* `product` es un `PhysicalProduct` que pertenece al `requestingUser` (cuando aplica).
* `warehouse` corresponde al `requestingUser` (propiedad o asignación, según §3).
* No existe ya un `Inventory` para esa combinación `product` + `warehouse` (de lo contrario, esta operación debe rechazarse; para ajustar cantidad se usa `Register Stock Movement` con `ADJUSTMENT`).
* `initialQuantity >= 0`.

## 14.4 Domain Creation

```text
Inventory
      |
      v
Validate
      |
      v
Create Domain State (movementType = STOCK_IN, availableQuantity = initialQuantity)
      |
      v
InventoryRepositoryPort.save(inventory)
```

---

# 15. Consult Inventory

## 15.1 Purpose

Recupera el registro de `Inventory` asociado a un producto y una warehouse.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Inventory.
3. Validate Inventory existence.
4. Validate authorization: Seller dueño del producto/warehouse, LogisticsOperator asignado, Admin, o Supervisor (lectura).
5. Return Inventory.
```

Un `Buyer` nunca puede invocar este servicio, ni siquiera para su propia visibilidad de disponibilidad — esa información, si se expone al comprador, debe resolverse a través de un servicio de catálogo (por ejemplo, "disponible"/"agotado" en `Product Catalog Management`), nunca exponiendo directamente el `Inventory`.

---

# 16. Reserve Stock

## 16.1 Purpose

Reserva la cantidad de un `PhysicalProduct` necesaria para atender un `Order` pendiente, garantizando que no se sobrevenda el stock.

## 16.2 Required Validations

* `requestingUser` es el sistema actuando en nombre del flujo de `Order Management` (normalmente invocado internamente por `Create Order`, no directamente por un `Buyer`).
* `Inventory` existe.
* `quantity > 0`.
* `quantity <= availableQuantity` — de lo contrario, `InsufficientStockException`.

## 16.3 Domain Behavior

```java
inventory.reserve(quantity);
```

## 16.4 Effect

```text
availableQuantity -= quantity
movementType = RESERVATION
```

## 16.5 Relación con Order Management

Este servicio es invocado por `Order Management` (`Create Order`) como parte del flujo de creación de pedido — no es una operación que un `Buyer` dispare directamente. La coordinación entre subdominios debe darse a través de los respectivos Input Ports, nunca accediendo `Order Management` directamente al `InventoryRepositoryPort`.

---

# 17. Release Reserved Stock

## 17.1 Purpose

Libera stock previamente reservado y lo devuelve al inventario disponible, por ejemplo cuando un `Order` se cancela antes de completarse.

## 17.2 Required Validations

* `Inventory` existe.
* `quantity > 0`.
* La cantidad a liberar no debe exceder lo que efectivamente fue reservado para ese `Order` (el servicio debe rastrear esa relación, no asumir un valor arbitrario).

## 17.3 Domain Behavior

```java
inventory.releaseReservation(quantity);
```

## 17.4 Effect

```text
availableQuantity += quantity
```

---

# 18. Register Stock Movement

## 18.1 Purpose

Registra un movimiento de inventario (`STOCK_IN`, `SALE_OUTBOUND`, `ADJUSTMENT` o `RETURN_INBOUND`) sobre un registro de `Inventory`.

## 18.2 Required Validations

* `requestingUser` es el `Seller`/`LogisticsOperator` autorizado sobre la warehouse, o `ADMIN`.
* `Inventory` existe.
* Para `SALE_OUTBOUND`: `quantity <= availableQuantity`.
* Para `STOCK_IN`, `ADJUSTMENT` (positivo) y `RETURN_INBOUND`: `quantity > 0`.
* Para `ADJUSTMENT` (negativo, corrección a la baja): el resultado no puede dejar `availableQuantity < 0`.

## 18.3 Domain Behavior

```java
inventory.registerStockIn(quantity);
inventory.registerSaleOutbound(quantity);
inventory.registerAdjustment(quantity);
inventory.registerReturnInbound(quantity);
```

El método específico invocado depende del `MovementType` solicitado — el servicio no debe tener una única función genérica `applyMovement(type, quantity)` que reimplemente la lógica de cada caso con condicionales; cada movimiento tiene su propio método de dominio con sus propias reglas.

## 18.4 Relación con Return Management

`RETURN_INBOUND` es invocado por `Returns and Refunds Management` (`Complete Return`) cuando un producto devuelto se reintegra físicamente al inventario — no es una operación que se dispare de forma aislada sin un `Return` completado de por medio.

---

# 19. Exception Model

```text
InventoryNotFoundException
DuplicateInventoryRecordException
InsufficientStockException
InvalidStockQuantityException
UnauthorizedInventoryOperationException
InvalidMovementTypeException
InvalidUserStatusException
```

---

# 20. Persistence Boundary

```text
InventoryManagementService
        |
        v
InventoryRepositoryPort
        |
        v
InventoryPersistenceAdapter
        |
        v
Database
```

---

# 21. Controller Responsibilities

Los controllers no deben implementar:

* Cálculo de `availableQuantity`.
* Validación de que la cantidad reservada no exceda el stock.
* Reglas de autorización sobre warehouse.
* Persistencia.

---

# 22. Validation Matrix

| Validation | Register Initial Stock | Consult | Reserve | Release | Register Movement |
|---|---:|---:|---:|---:|---:|
| Requesting User | Yes | Yes | Yes (interno, Order flow) | Yes (interno, Order flow) | Yes |
| Authorization (warehouse) | Yes | Yes | N/A (invocado internamente) | N/A (invocado internamente) | Yes |
| Inventory existence | N/A (se crea) | Yes | Yes | Yes | Yes |
| No duplicate record | Yes | N/A | N/A | N/A | N/A |
| Non-negative quantity | Yes (inicial >= 0) | N/A | Yes (no exceder disponible) | Yes | Yes (según tipo) |

---

# 23. Business Rules Summary

## BR-001 — Inventory Must Link a PhysicalProduct and a Warehouse

Nunca puede existir un `Inventory` sin ambos.

## BR-002 — Only PhysicalProduct Has Inventory

Un `DigitalProduct` nunca tiene un `Inventory` asociado.

## BR-003 — Stock Can Never Be Negative

`availableQuantity >= 0` en todo momento — es la regla más crítica del subdominio.

## BR-004 — Movement Type Reflects the Operation, Not a Free Choice

Cada servicio corresponde a un `MovementType` específico determinado por su propia lógica.

## BR-005 — One Inventory Record per Product-Warehouse Pair

No se permiten registros duplicados para la misma combinación.

## BR-006 — Domain State Changes Through Domain Behavior

## BR-007 — External Information Uses Output Ports

## BR-008 — Services Must Be Cohesive

---

# 24. Anti-Patterns

## 24.1 Direct Database Access

## 24.2 Primitive Application Contracts

## 24.3 Trusting Caller-Supplied Available Quantity

Nunca decidir si una reserva o salida es posible basándose en un `availableQuantity` enviado por el caller sin recuperar el estado autoritativo primero.

## 24.4 Manual Quantity Calculation

Inválido:

```java
inventory.setAvailableQuantity(
    inventory.getAvailableQuantity() - quantity
);
```

Prefiere:

```java
inventory.registerSaleOutbound(quantity);
```

## 24.5 A Single Generic applyMovement Method

Inválido:

```java
inventory.applyMovement(movementType, quantity); // con un switch interno gigante
```

Cada tipo de movimiento tiene su propio método de dominio con sus propias reglas — no deben mezclarse en un único método genérico que reimplemente condicionalmente la lógica de cada uno.

## 24.6 Cross-Subdomain Direct Access

Inválido: que `Order Management` acceda directamente a `InventoryRepositoryPort` en vez de invocar `ReserveStockUseCase`.

---

# 25. Testing Requirements

## 25.1 Register Initial Stock Tests

* Registro válido.
* Cantidad negativa (rechazada).
* Registro duplicado para el mismo product-warehouse (rechazado).
* `DigitalProduct` (rechazado — no aplica inventario).

## 25.2 Consult Inventory Tests

* Seller dueño consulta su inventario (permitido).
* LogisticsOperator asignado consulta (permitido).
* Buyer intenta consultar (rechazado).

## 25.3 Reserve Stock Tests

* Reserva válida con stock suficiente.
* Reserva que excede el stock disponible (rechazada, `InsufficientStockException`).
* Reserva concurrente que en conjunto excedería el stock (la segunda debe rechazarse contra el estado autoritativo actualizado).

## 25.4 Release Reserved Stock Tests

* Liberación válida.
* Intento de liberar más de lo reservado (rechazado).

## 25.5 Register Stock Movement Tests

* `STOCK_IN` válido.
* `SALE_OUTBOUND` que excede disponible (rechazado).
* `ADJUSTMENT` positivo y negativo, este último sin dejar cantidad negativa.
* `RETURN_INBOUND` válido.

---

# 26. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models y Value Objects apropiados (`MovementType` como Value Object).
* No expone DTOs REST como contratos de aplicación.
* Recupera el `Inventory` autoritativo antes de mutar cantidad.
* Valida al usuario solicitante y su relación con la warehouse.
* Garantiza `availableQuantity >= 0` en todo momento.
* Ejecuta comportamiento de dominio válido, un método por tipo de movimiento.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura, incluyendo casos de concurrencia sobre el mismo registro.

---

# 27. Final Service Catalog

```text
Inventory Management
|
+-- Register Initial Stock
|
+-- Consult Inventory
|
+-- Reserve Stock
|
+-- Release Reserved Stock
|
+-- Register Stock Movement
```

---

# 28. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando siempre contra el estado autoritativo del Inventory y garantizando que `availableQuantity` nunca sea negativo, ejecutando el método de dominio correspondiente al tipo de movimiento, y persistiendo a través de Output Ports.**