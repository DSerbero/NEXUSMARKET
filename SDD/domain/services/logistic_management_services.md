# Logistics Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Logistics Management** del sistema NexusMarket.

El subdominio Logistics Management es responsable del proceso físico de empaque, despacho y transporte de los pedidos que contienen productos físicos.

Las principales capacidades de negocio son:

* Register Shipment.
* Update Shipment Status.
* Consult Shipment.

El envío está representado por el Domain Model `Shipment`.

```text
Shipment
  |
  +-- order : Order
  +-- originWarehouse : Warehouse
  +-- operator : LogisticsOperator
  +-- shipmentStatus : ShipmentStatus
  +-- dispatchDate : LocalDateTime
```

Un `Shipment` siempre se origina a partir de un `Order` que avanza a `DISPATCHED` — no existe un `Shipment` independiente de un `Order` (mismo patrón de relación 1:1 que ya vimos entre `Order` e `Invoice`).

```text
Order
 |
 | dispatch()
 v
Order Management invoca RegisterShipmentUseCase
 |
 v
Shipment
```

> **Recordatorio de alcance:** un `Order` compuesto exclusivamente por `DigitalProduct` nunca genera un `Shipment` (ver Order Management Services, §17.4) — este subdominio solo aplica a pedidos con al menos un `PhysicalProduct`.

---

# 2. Domain Model Context

## 2.1 Shipment

```text
Shipment
  |
  +-- order : Order
  +-- originWarehouse : Warehouse
  +-- operator : LogisticsOperator
  +-- shipmentStatus : ShipmentStatus
  +-- dispatchDate : LocalDateTime
```

## 2.2 Shipment Status Lifecycle

```text
IN_PREPARATION
      |
      v
  DISPATCHED
      |
      v
  IN_TRANSIT
      |
      v
   DELIVERED
```

`DELIVERED` es un estado terminal para el `Shipment` — su llegada dispara `Finalize Order` en Order Management (ver §15.5).

---

# 3. LogisticsOperator and Shipment Relationship

```text
Shipment.operator : LogisticsOperator
```

Solo el `LogisticsOperator` asignado como `operator` del `Shipment` (que a su vez debe corresponder al `responsibleUser` de `originWarehouse`, ver Warehouse Management Services) puede actualizar su estado. `ADMIN` puede operar sin restricción; `SUPERVISOR` solo lectura; el `Buyer` dueño del `Order` solo puede consultar, nunca modificar.

```text
Shipment.operator == Shipment.originWarehouse.responsibleUser
```

Esta igualdad no es automática por el tipo de los datos — debe validarse explícitamente al registrar el `Shipment` (mismo principio de "el tipo no garantiza la regla de negocio" que ya aplicamos en Warehouse Management).

---

# 4. Service Design Principle

Cada servicio de Logistics Management representa una operación de negocio cohesiva.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative Shipment (o Order, para Register Shipment)
                |
                v
Validate requesting User (LogisticsOperator asignado, o Admin)
                |
                v
Validate status transition
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
updateShipmentStatus(
    String shipmentId,
    String newStatus
);
```

Preferido:

```java
updateShipmentStatus(
    User requestingUser,
    Shipment shipment,
    ShipmentStatus newStatus
);
```

---

# 7. Authoritative State

```text
Input Shipment
       |
       v
ShipmentRepositoryPort
       |
       v
Authoritative Shipment
       |
       v
Business Validation
```

El servicio nunca debe confiar en `shipmentStatus` suministrado por el caller para decidir si una transición es válida.

---

# 8. External Information

* `WarehouseRepositoryPort` — para validar que `operator == originWarehouse.responsibleUser`.
* `OrderRepositoryPort` — para validar que `order.orderStatus == DISPATCHED` al registrar el shipment.

Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Shipment Validation

* `order` existe y `order.orderStatus == DISPATCHED` al momento de registrar el shipment.
* `order` contiene al menos un `PhysicalProduct` (un order 100% digital nunca debe llegar a este subdominio, ver §1).
* `originWarehouse` existe.
* `operator.role == LOGISTICS_OPERATOR` y `operator == originWarehouse.responsibleUser`.
* No existe ya un `Shipment` para ese `Order` (uno a uno).

---

# 10. Domain Behavior

Preferido:

```java
shipment.markDispatched();
shipment.markInTransit();
shipment.markDelivered();
```

Evitar:

```java
shipment.setShipmentStatus(...);
```

---

# 11. Input Ports

```text
RegisterShipmentUseCase
UpdateShipmentStatusUseCase
ConsultShipmentUseCase
```

---

# 12. Output Ports

```text
ShipmentRepositoryPort
WarehouseRepositoryPort
OrderRepositoryPort
```

## Service-to-Port Matrix

| Service | ShipmentRepositoryPort | WarehouseRepositoryPort | OrderRepositoryPort |
|---|---:|---:|---:|
| Register Shipment | ✓ (save) | ✓ (validar operator) | ✓ (validar order.orderStatus) |
| Update Shipment Status | ✓ (update) | | |
| Consult Shipment | ✓ (read) | | |

---

# 13. ShipmentRepositoryPort

```java
public interface ShipmentRepositoryPort {

    Shipment save(Shipment shipment);

    Optional<Shipment> findByOrder(Shipment shipment);

    Optional<Shipment> findByIdentifier(Shipment shipment);

    List<Shipment> findByOperator(Shipment shipment);

    Shipment update(Shipment shipment);
}
```

`findByOperator` respalda la vista operativa diaria de un `LogisticsOperator` (sus envíos activos).

---

# 14. Register Shipment

## 14.1 Purpose

Crea el registro de un `Shipment` para un `Order` que contiene productos físicos, vinculándolo a la warehouse de origen y al operador responsable.

Este servicio es invocado por `Order Management` (`Dispatch Order`), no directamente por un `LogisticsOperator` a través de un endpoint separado.

## 14.2 Input

```text
Order (dispatchedOrder)
Warehouse (originWarehouse)
LogisticsOperator (operator)
```

## 14.3 Validations

Ver §9.

## 14.4 Domain Creation

```text
Order
      |
      v
Validate
      |
      v
Create Shipment (shipmentStatus = IN_PREPARATION, dispatchDate = null)
      |
      v
ShipmentRepositoryPort.save(shipment)
```

`dispatchDate` se establece cuando el shipment transiciona efectivamente a `DISPATCHED` (§15.4), no en el momento de su creación en `IN_PREPARATION`.

---

# 15. Update Shipment Status

## 15.1 Purpose

Actualiza el estado de un `Shipment` a medida que avanza por las etapas de preparación, despacho, tránsito y entrega.

## 15.2 Required Validations

* `requestingUser == Shipment.operator` o `requestingUser.role == ADMIN`.
* `Shipment` existe.
* Transición de `shipmentStatus` válida, estrictamente secuencial (§2.2).

## 15.3 Domain Behavior

```java
shipment.markDispatched();
shipment.markInTransit();
shipment.markDelivered();
```

## 15.4 Dispatch Date

Al transicionar a `DISPATCHED`, el servicio debe fijar `dispatchDate = now()` — no antes, no como parte de `Register Shipment`.

## 15.5 Coordination with Order Management

Cuando `Shipment` alcanza `DELIVERED`, este servicio debe invocar `FinalizeOrderUseCase` (Order Management) para avanzar el `Order` asociado a `DELIVERED_FINALIZED`. La relación es análoga a `Confirm Cart -> CreateOrderUseCase`: `Logistics Management` coordina, no fusiona responsabilidades con `Order Management`.

```text
shipment.markDelivered()
      |
      v
ShipmentRepositoryPort.update(shipment)
      |
      v
FinalizeOrderUseCase.execute(shipment.order)
```

---

# 16. Consult Shipment

## 16.1 Purpose

Recupera la información de un `Shipment` de acuerdo con los permisos del usuario solicitante.

## 16.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Shipment.
3. Validate Shipment existence.
4. Validate authorization: operator asignado, Buyer dueño del Order relacionado, Admin, o Supervisor.
5. Return Shipment.
```

---

# 17. Exception Model

```text
ShipmentNotFoundException
DuplicateShipmentException
InvalidShipmentStatusTransitionException
InvalidShipmentOperatorException
OrderNotEligibleForShipmentException
UnauthorizedShipmentOperationException
```

---

# 18. Persistence Boundary

```text
LogisticsManagementService
        |
        v
ShipmentRepositoryPort
        |
        v
ShipmentPersistenceAdapter
        |
        v
Database
```

---

# 19. Controller Responsibilities

Los controllers no deben implementar:

* Validación de `operator == originWarehouse.responsibleUser`.
* Reglas de transición de estado.
* Coordinación con `FinalizeOrderUseCase`.
* Persistencia.

---

# 20. Validation Matrix

| Validation | Register | Update Status | Consult |
|---|---:|---:|---:|
| Requesting User | N/A (interno, vía Order) | Yes | Yes |
| Order eligibility (DISPATCHED, contiene físicos) | Yes | N/A | N/A |
| Operator == responsibleUser | Yes | N/A (ya validado al registrar) | N/A |
| Shipment existence | N/A (se crea) | Yes | Yes |
| Status transition validity | N/A (estado inicial) | Yes | N/A |
| Finalize Order coordination | N/A | Yes (solo en DELIVERED) | N/A |

---

# 21. Business Rules Summary

## BR-001 — One Shipment per Order

## BR-002 — Shipment Only Exists for Orders with Physical Products

## BR-003 — Operator Must Match the Origin Warehouse's Responsible User

Validación de negocio explícita, no garantizada por el tipo.

## BR-004 — Shipment Status Is Strictly Sequential

## BR-005 — Delivered Shipment Triggers Order Finalization

## BR-006 — Register Shipment Is Triggered by Order Management, Not Standalone

## BR-007 — Domain State Changes Through Domain Behavior

## BR-008 — Services Must Be Cohesive

---

# 22. Anti-Patterns

## 22.1 Direct Database Access

## 22.2 Primitive Application Contracts

## 22.3 Trusting Caller-Supplied Shipment Status

## 22.4 Assigning an Operator Not Responsible for the Origin Warehouse

## 22.5 Shipment Directly Mutating Order

Inválido:

```java
shipment.markDelivered();
order.setOrderStatus(DELIVERED_FINALIZED); // acceso directo, sin pasar por Order Management
```

Debe usarse `FinalizeOrderUseCase`.

---

# 23. Testing Requirements

## 23.1 Register Shipment Tests

* Registro válido para order `DISPATCHED` con productos físicos.
* Intento de registrar para order sin productos físicos (rechazado).
* Intento de registrar con operator que no es responsable de la warehouse (rechazado).
* Segundo shipment para el mismo order (rechazado).

## 23.2 Update Shipment Status Tests

* `IN_PREPARATION -> DISPATCHED` (permitido, fija `dispatchDate`).
* `DISPATCHED -> IN_TRANSIT` (permitido).
* `IN_TRANSIT -> DELIVERED` (permitido, dispara `FinalizeOrderUseCase`).
* Salto de estado, p. ej. `IN_PREPARATION -> DELIVERED` (rechazado).
* Operador no asignado intenta actualizar (rechazado).

## 23.3 Consult Shipment Tests

* Operator asignado consulta (permitido).
* Buyer dueño del order consulta (permitido).
* Otro buyer intenta consultar (rechazado).

---

# 24. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models apropiados.
* No expone DTOs REST como contratos de aplicación.
* Valida la relación operator-warehouse explícitamente.
* Garantiza la secuencialidad estricta del ciclo de vida del shipment.
* Coordina con Order Management al alcanzar `DELIVERED`.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 25. Final Service Catalog

```text
Logistics Management
|
+-- Register Shipment
|
+-- Update Shipment Status
|
+-- Consult Shipment
```

---

# 26. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando la relación entre el LogisticsOperator y la Warehouse de origen y la secuencialidad estricta del ciclo de vida del Shipment, coordinando con Order Management al alcanzar la entrega, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**