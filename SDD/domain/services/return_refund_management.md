# Returns and Refunds Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Returns and Refunds Management** del sistema NexusMarket.

El subdominio es responsable del proceso posventa: la devolución de un producto ya entregado y el reembolso financiero correspondiente.

Las principales capacidades de negocio son:

* Request Return.
* Consult Return.
* Approve Return.
* Reject Return.
* Complete Return.
* Process Refund.
* Consult Refund.

> **Nota de alcance:** `Consult Return` no aparecía en el catálogo general de servicios (`Services_NexusMarket.md`), pero es necesaria para que un buyer o un administrador puedan revisar el estado de una solicitud antes de que exista un `Refund` — se agrega aquí como un complemento evidente, siguiendo el mismo patrón de `Consult X` presente en todos los demás subdominios. Vale la pena reflejarla también en el catálogo general.

Las devoluciones y reembolsos están representados por dos Domain Models distintos:

```text
Return
  |
  +-- order : Order
  +-- reason : String
  +-- returnStatus : ReturnStatus
  +-- requestDate : LocalDateTime

Refund
  |
  +-- relatedReturn : Return
  +-- refundedAmount : BigDecimal
  +-- refundStatus : RefundStatus
  +-- processingDate : LocalDateTime
```

Un `Return` se origina a partir de un `Order` finalizado, sin reabrirlo ni modificarlo (ver Order Management Services, §18.4). Un `Refund`, a su vez, se origina a partir de un `Return` completado.

```text
Order (DELIVERED_FINALIZED)
 |
 | Request Return
 v
Return (REQUESTED -> APPROVED -> COMPLETED)
 |
 | Process Refund
 v
Refund
```

---

# 2. Domain Model Context

## 2.1 Return Status Lifecycle

```text
REQUESTED
    |
    ├── APPROVED --> COMPLETED
    |
    └── REJECTED   (terminal)
```

`REJECTED` y `COMPLETED` son ambos estados terminales para `Return` — no hay transición posterior desde ninguno de los dos.

## 2.2 Refund Status Lifecycle

```text
PENDING
   |
   v
PROCESSED

PENDING
   |
   v
REJECTED
```

## 2.3 One Active Return per Order

Este documento asume que un `Order` solo puede tener un `Return` activo a la vez (no `REJECTED`) — no se permite solicitar una nueva devolución mientras exista una `REQUESTED` o `APPROVED` pendiente sobre el mismo `Order`. Si un `Return` fue `REJECTED`, el buyer podría, en principio, solicitar uno nuevo con una justificación distinta; esta última regla es una interpretación razonable, no algo explícito en la especificación, y debe confirmarse.

## 2.4 refundedAmount Is Derived, Not Supplied

Igual que con `Invoice.totalAmount`, `Refund.refundedAmount` no debe ser un valor arbitrario del caller. Este documento asume reembolso total: `refundedAmount = Invoice.totalAmount` del `Order` asociado, dado que el Domain Model no tiene granularidad a nivel de línea de producto dentro de `Order`/`Return` que permita calcular un reembolso parcial. Un reembolso parcial (por ejemplo, devolver solo uno de varios productos de un mismo pedido) **no está soportado** por el modelo actual — es una limitación real que debe señalarse, no resolverse silenciosamente asumiendo algo que el modelo no expresa.

---

# 3. Buyer, Admin, LogisticsOperator, and Return Relationship

```text
Requesting User
 |
 ├── Buyer                    -> solo puede solicitar/consultar Return sobre su propio Order
 ├── Admin/LogisticsOperator  -> puede aprobar, rechazar y completar cualquier Return
 └── Admin                    -> único rol que puede ejecutar Process Refund
```

Esta distribución de roles ya fue establecida en `Authorization Services` (§7, §8 y §14 de ese documento) — este documento la aplica concretamente a cada servicio.

---

# 4. Service Design Principle

Cada servicio de este subdominio representa una operación de negocio cohesiva, coordinando —sin fusionar responsabilidades— con Order Management (para validar elegibilidad) e Inventory Management (para reintegrar stock).

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative Return / Refund / Order
                |
                v
Validate requesting User and role
                |
                v
Validate status transition
                |
                v
Coordinate with Order/Inventory Management (cuando aplica)
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
processRefund(String returnId, BigDecimal amount);
```

Preferido:

```java
processRefund(
    User requestingAdmin,
    Return completedReturn
);
```

`refundedAmount` nunca se recibe como parámetro (§2.4).

---

# 7. Authoritative State

```text
Input Return / Refund
       |
       v
ReturnRepositoryPort / RefundRepositoryPort
       |
       v
Authoritative state
       |
       v
Business Validation
```

---

# 8. External Information

* `OrderRepositoryPort` — validar que `order.orderStatus == DELIVERED_FINALIZED` al solicitar un return.
* `InvoiceRepositoryPort` — obtener `totalAmount` para calcular `refundedAmount`.
* `RegisterStockMovementUseCase` (Inventory Management) — reintegrar stock al completar un return de productos físicos.

Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Return and Refund Validation

* `order.orderStatus == DELIVERED_FINALIZED` al solicitar el return.
* `reason` no está vacío.
* No existe ya un `Return` activo (`REQUESTED`/`APPROVED`) para ese `Order` (§2.3).
* `Refund` solo puede procesarse sobre un `Return` con `returnStatus == COMPLETED`.
* No existe ya un `Refund` para ese `Return` (uno a uno).

---

# 10. Domain Behavior

Preferido:

```java
returnRequest.approve();
returnRequest.reject();
returnRequest.complete();
refund.markProcessed();
refund.markRejected();
```

Evitar:

```java
returnRequest.setReturnStatus(...);
refund.setRefundStatus(...);
```

---

# 11. Input Ports

```text
RequestReturnUseCase
ConsultReturnUseCase
ApproveReturnUseCase
RejectReturnUseCase
CompleteReturnUseCase
ProcessRefundUseCase
ConsultRefundUseCase
```

---

# 12. Output Ports

```text
ReturnRepositoryPort
RefundRepositoryPort
OrderRepositoryPort
InvoiceRepositoryPort
```

## Service-to-Port Matrix

| Service | ReturnRepositoryPort | RefundRepositoryPort | OrderRepositoryPort | InvoiceRepositoryPort | Cross-subdomain |
|---|---:|---:|---:|---:|---:|
| Request Return | ✓ (save) | | ✓ (validar DELIVERED_FINALIZED) | | |
| Consult Return | ✓ (read) | | | | |
| Approve Return | ✓ (update) | | | | |
| Reject Return | ✓ (update) | | | | |
| Complete Return | ✓ (update) | | | | `RegisterStockMovementUseCase` |
| Process Refund | ✓ (read) | ✓ (save) | | ✓ (obtener totalAmount) | |
| Consult Refund | | ✓ (read) | | | |

---

# 13. ReturnRepositoryPort y RefundRepositoryPort

```java
public interface ReturnRepositoryPort {

    Return save(Return returnRequest);

    Optional<Return> findByIdentifier(Return returnRequest);

    Optional<Return> findActiveByOrder(Return returnRequest);

    Return update(Return returnRequest);
}
```

```java
public interface RefundRepositoryPort {

    Refund save(Refund refund);

    Optional<Refund> findByReturn(Refund refund);

    Optional<Refund> findByIdentifier(Refund refund);
}
```

`findActiveByOrder` respalda la regla de "un solo return activo por order" (§2.3). `findByReturn` respalda la regla de "un solo refund por return" (§9).

---

# 14. Request Return

## 14.1 Purpose

Registra la solicitud de un buyer para devolver un producto ya entregado e inicia el ciclo de vida del return.

## 14.2 Input

```text
Buyer (requestingBuyer)
Order (deliveredOrder)
String (reason)
```

## 14.3 Validations

* `requestingBuyer == deliveredOrder.buyer`.
* `deliveredOrder.orderStatus == DELIVERED_FINALIZED`.
* No existe ya un `Return` activo para `deliveredOrder`.
* `reason` no está vacío.

## 14.4 Domain Creation

```text
Order
      |
      v
Validate
      |
      v
Create Return (returnStatus = REQUESTED, requestDate = now)
      |
      v
ReturnRepositoryPort.save(returnRequest)
```

---

# 15. Consult Return

## 15.1 Purpose

Recupera la información de un `Return` de acuerdo con los permisos del usuario solicitante.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Return.
3. Validate Return existence.
4. Validate authorization: Buyer dueño del Order relacionado, Admin, LogisticsOperator, o Supervisor.
5. Return Return.
```

---

# 16. Approve Return

## 16.1 Purpose

Aprueba una solicitud de return tras validar las condiciones del inventario y del order relacionados.

## 16.2 Required Validations

* `requestingUser.role in {ADMIN, LOGISTICS_OPERATOR}`.
* `Return` existe.
* `Return.returnStatus == REQUESTED`.

## 16.3 Domain Behavior

```java
returnRequest.approve();
```

## 16.4 Relation to Inventory

Este servicio **no** reintegra inventario — eso ocurre únicamente en `Complete Return` (§18), una vez que el producto físico efectivamente regresó a la warehouse. Aprobar es una decisión, completar es un hecho físico consumado; no deben fusionarse.

---

# 17. Reject Return

## 17.1 Purpose

Rechaza una solicitud de return y registra la decisión correspondiente.

## 17.2 Required Validations

* `requestingUser.role in {ADMIN, LOGISTICS_OPERATOR}`.
* `Return` existe.
* `Return.returnStatus == REQUESTED`.

## 17.3 Domain Behavior

```java
returnRequest.reject();
```

`REJECTED` es terminal — no admite una reconsideración posterior sobre el mismo `Return` (§2.1).

---

# 18. Complete Return

## 18.1 Purpose

Finaliza un return aprobado reintegrando el producto devuelto al inventario.

## 18.2 Required Validations

* `requestingUser.role in {ADMIN, LOGISTICS_OPERATOR}`.
* `Return` existe.
* `Return.returnStatus == APPROVED`.

## 18.3 Domain Behavior and Inventory Coordination

```text
returnRequest.complete()
      |
      v
ReturnRepositoryPort.update(returnRequest)
      |
      v
Para cada Product físico en Order.items:
      |
      v
RegisterStockMovementUseCase.execute(product, warehouse, quantity, RETURN_INBOUND)
```

Si `Order` contiene únicamente productos digitales, este paso de reintegración de inventario no aplica — completar el return es entonces una operación puramente administrativa/documental (habilita el `Refund`, pero no toca `Inventory`).

---

# 19. Process Refund

## 19.1 Purpose

Emite un reembolso financiero al buyer como resultado de un return aprobado y completado.

## 19.2 Required Validations

* `requestingUser.role == ADMIN`.
* `Return.returnStatus == COMPLETED`.
* No existe ya un `Refund` para ese `Return`.

## 19.3 Domain Creation

```text
Return (COMPLETED)
      |
      v
Validate
      |
      v
Retrieve Invoice.totalAmount (a través de InvoiceRepositoryPort)
      |
      v
Create Refund (refundedAmount = Invoice.totalAmount, refundStatus = PENDING)
      |
      v
RefundRepositoryPort.save(refund)
      |
      v
refund.markProcessed()  // o markRejected() si el proceso de pago externo falla
```

## 19.4 Payment Gateway

El mecanismo técnico de devolución de dinero (pasarela de pago) está fuera del alcance de este subdominio — este servicio asume que existe un adaptador externo responsable de ejecutar la devolución real, y que su resultado determina si `Refund` termina en `PROCESSED` o `REJECTED`.

---

# 20. Consult Refund

## 20.1 Purpose

Recupera la información de un `Refund` de acuerdo con los permisos del usuario solicitante.

## 20.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Refund.
3. Validate Refund existence.
4. Validate authorization: Buyer dueño del Return/Order relacionado, Admin, o Supervisor.
5. Return Refund.
```

---

# 21. Exception Model

```text
ReturnNotFoundException
RefundNotFoundException
OrderNotEligibleForReturnException
DuplicateActiveReturnException
InvalidReturnStatusTransitionException
InvalidRefundStatusTransitionException
DuplicateRefundException
UnauthorizedReturnOperationException
UnauthorizedRefundOperationException
```

---

# 22. Persistence Boundary

```text
ReturnsRefundsManagementService
        |
        v
ReturnRepositoryPort / RefundRepositoryPort
        |
        v
Persistence Adapter
        |
        v
Database
```

---

# 23. Controller Responsibilities

Los controllers no deben implementar:

* Cálculo de `refundedAmount`.
* Reglas de transición de estado de `Return`/`Refund`.
* Coordinación con Inventory Management.
* Persistencia.

---

# 24. Validation Matrix

| Validation | Request | Consult Return | Approve | Reject | Complete | Process Refund | Consult Refund |
|---|---:|---:|---:|---:|---:|---:|---:|
| Requesting User | Yes (Buyer) | Yes | Yes (Admin/Operator) | Yes (Admin/Operator) | Yes (Admin/Operator) | Yes (Admin) | Yes |
| Order eligibility (DELIVERED_FINALIZED) | Yes | N/A | N/A | N/A | N/A | N/A | N/A |
| No duplicate active Return | Yes | N/A | N/A | N/A | N/A | N/A | N/A |
| Return existence | N/A (se crea) | Yes | Yes | Yes | Yes | Yes | N/A |
| Status transition validity | N/A | N/A | Yes (REQUESTED) | Yes (REQUESTED) | Yes (APPROVED) | Yes (COMPLETED) | N/A |
| Inventory reintegration | N/A | N/A | N/A | N/A | Yes (si aplica) | N/A | N/A |
| No duplicate Refund | N/A | N/A | N/A | N/A | N/A | Yes | N/A |

---

# 25. Business Rules Summary

## BR-001 — Return Never Reopens the Order

`Order` permanece inmutable; `Return` es una entidad separada asociada.

## BR-002 — One Active Return per Order

## BR-003 — Approval Is a Decision, Completion Is a Physical Fact

Reintegración de inventario ocurre solo en `Complete Return`, nunca en `Approve Return`.

## BR-004 — Refund Requires a Completed Return

## BR-005 — refundedAmount Is Always Derived From Invoice

No soporta reembolso parcial en el modelo actual (limitación señalada, no resuelta).

## BR-006 — Only Admin Processes Refunds

## BR-007 — Domain State Changes Through Domain Behavior

## BR-008 — Services Must Be Cohesive

---

# 26. Anti-Patterns

## 26.1 Direct Database Access

## 26.2 Primitive Application Contracts

## 26.3 Accepting refundedAmount from the Caller

## 26.4 Reintegrating Inventory at Approval Instead of Completion

Inválido: llamar `RegisterStockMovementUseCase` dentro de `Approve Return`.

## 26.5 Reopening the Order

Inválido: cualquier código que, al procesar un `Return`, modifique `Order.orderStatus`.

## 26.6 Processing a Refund Without a Completed Return

---

# 27. Testing Requirements

## 27.1 Request Return Tests

* Solicitud válida sobre order `DELIVERED_FINALIZED`.
* Intento sobre order no finalizado (rechazado).
* Segunda solicitud mientras existe una activa (rechazada).

## 27.2 Approve / Reject Return Tests

* Aprobación válida desde `REQUESTED`.
* Rechazo válido desde `REQUESTED`.
* Intento de aprobar/rechazar un return ya `APPROVED`/`REJECTED`/`COMPLETED` (rechazado).
* Buyer intenta aprobar su propio return (rechazado).

## 27.3 Complete Return Tests

* Completar return con productos físicos (reintegra inventario, verificado).
* Completar return con productos digitales (no reintegra inventario).
* Intento de completar sin estar `APPROVED` (rechazado).

## 27.4 Process Refund Tests

* Procesamiento válido para return `COMPLETED`.
* Intento de procesar para return no `COMPLETED` (rechazado).
* Segundo refund para el mismo return (rechazado).
* `refundedAmount` calculado coincide con `Invoice.totalAmount`.
* Buyer intenta procesar su propio refund (rechazado).

---

# 28. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models apropiados.
* No expone DTOs REST como contratos de aplicación.
* Nunca reabre ni modifica el `Order` original.
* Separa claramente aprobación (decisión) de completado (hecho físico).
* Calcula `refundedAmount` siempre desde `Invoice`, nunca lo recibe como input.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 29. Final Service Catalog

```text
Returns and Refunds Management
|
+-- Request Return
|
+-- Consult Return
|
+-- Approve Return
|
+-- Reject Return
|
+-- Complete Return
|
+-- Process Refund
|
+-- Consult Refund
```

---

# 30. Pending Domain Limitation

El modelo actual no soporta reembolsos ni devoluciones parciales a nivel de producto individual dentro de un `Order` con múltiples items — `Return` y `Refund` operan siempre sobre el pedido completo. Si el negocio requiere devoluciones parciales, esto exige agregar granularidad a nivel de línea de producto en `Order`/`Return`, lo cual está fuera del alcance actual del Domain Model.

---

# 31. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando la elegibilidad del Order, la secuencialidad estricta de Return y Refund, y coordinando —sin fusionar responsabilidades— con Order Management e Inventory Management, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**