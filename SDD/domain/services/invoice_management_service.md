# Invoicing Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Invoicing Management** del sistema NexusMarket.

El subdominio Invoicing Management es responsable de la información comercial y financiera asociada a las ventas confirmadas.

Las principales capacidades de negocio son:

* Issue Invoice.
* Consult Invoice.
* Void Invoice.

La factura está representada por el Domain Model `Invoice`.

```text
Invoice
  |
  +-- order : Order
  +-- totalAmount : BigDecimal
  +-- issueDate : LocalDateTime
  +-- invoiceStatus : InvoiceStatus
```

Una `Invoice` siempre se origina a partir de un `Order` cuyo pago fue confirmado — no existe una `Invoice` independiente de un `Order`.

```text
Order
 |
 | confirmPayment()
 v
Order Management invoca IssueInvoiceUseCase
 |
 v
Invoice
```

---

# 2. Domain Model Context

## 2.1 Invoice

```text
Invoice
  |
  +-- order : Order
  +-- totalAmount : BigDecimal
  +-- issueDate : LocalDateTime
  +-- invoiceStatus : InvoiceStatus
```

## 2.2 Invoice Status Lifecycle

```text
ISSUED
   |
   v
 PAID
   |
   v
VOIDED
```

`ISSUED` es el estado inicial (una `Invoice` se crea ya con el pago del `Order` confirmado, así que en la práctica `ISSUED` y `PAID` pueden coincidir temporalmente — ver §14.3 para la aclaración de esta relación). `VOIDED` es un estado terminal.

## 2.3 totalAmount Is Derived, Not Supplied

`Invoice.totalAmount` no es un valor que el caller decida; se calcula a partir de `Order.items` en el momento de la emisión. Aceptar un `totalAmount` arbitrario del caller sería un riesgo de integridad financiera — el mismo tipo de error que ya evitamos en Inventory Management al no confiar en `availableQuantity` suministrado externamente.

---

# 3. Order and Invoice Relationship

```text
Order (orderStatus == PAID)
 |
 | origina
 v
Invoice (invoiceStatus == ISSUED)
```

Una `Invoice` es de solo lectura desde la perspectiva de cualquier actor externo al sistema — no se "edita" una factura ya emitida. La única operación de modificación de estado es `Void Invoice`, para casos excepcionales (p. ej., error administrativo), no una corrección de montos.

---

# 4. Service Design Principle

Cada servicio de Invoicing Management representa una operación de negocio cohesiva.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative Order (para Issue Invoice) o Invoice (para las demás)
                |
                v
Validate requesting context
                |
                v
Calculate totalAmount desde Order.items (solo en Issue Invoice)
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
issueInvoice(
    String orderId,
    BigDecimal totalAmount
);
```

Preferido:

```java
issueInvoice(
    Order order
);
```

`totalAmount` nunca se recibe como parámetro — se calcula internamente.

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
Calculate totalAmount
```

---

# 8. External Information

`OrderRepositoryPort` — para recuperar el `Order` autoritativo y sus `items` al calcular `totalAmount`. Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Invoice Validation

* `order.orderStatus == PAID` al momento de emitir la factura.
* No existe ya una `Invoice` para ese `Order` (una factura por pedido — emitir dos facturas para el mismo `Order` es una violación de integridad).
* `totalAmount` calculado es mayor que cero.

---

# 10. Domain Behavior

Preferido:

```java
invoice.markAsPaid();
invoice.voidInvoice();
```

Evitar:

```java
invoice.setInvoiceStatus(...);
invoice.setTotalAmount(...);
```

---

# 11. Input Ports

```text
IssueInvoiceUseCase
ConsultInvoiceUseCase
VoidInvoiceUseCase
```

---

# 12. Output Ports

```text
InvoiceRepositoryPort
OrderRepositoryPort
```

## Service-to-Port Matrix

| Service | InvoiceRepositoryPort | OrderRepositoryPort |
|---|---:|---:|
| Issue Invoice | ✓ (save) | ✓ (read, calcular totalAmount) |
| Consult Invoice | ✓ (read) | |
| Void Invoice | ✓ (update) | |

---

# 13. InvoiceRepositoryPort

```java
public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findByOrder(Invoice invoice);

    Optional<Invoice> findByIdentifier(Invoice invoice);

    Invoice update(Invoice invoice);
}
```

`findByOrder` es la operación central: se usa para garantizar que no se emitan dos facturas para el mismo `Order` (§9).

---

# 14. Issue Invoice

## 14.1 Purpose

Genera una `Invoice` para un `Order` confirmado, registrando la información comercial y financiera de la venta.

Este servicio es invocado por `Order Management` (`Confirm Order Payment`), no directamente por un actor externo (mismo patrón de coordinación ya usado entre Cart Management y Order Management).

## 14.2 Input

```text
Order (paidOrder)
```

## 14.3 Validations

* `paidOrder.orderStatus == PAID`.
* No existe ya una `Invoice` para `paidOrder` (`InvoiceRepositoryPort.findByOrder`).

## 14.4 Domain Creation

```text
Order
      |
      v
Validate
      |
      v
Calculate totalAmount = sum(Order.items[i].price for each item)
      |
      v
Create Invoice (invoiceStatus = ISSUED, issueDate = now)
      |
      v
InvoiceRepositoryPort.save(invoice)
```

> **Nota resuelta:** `Product.price` fue agregado al Domain Model (`Domain_Model_NexusMarket.md`) para soportar este cálculo. `totalAmount` se calcula sumando `price` de cada item en `Order.items` en el momento de la emisión.

---

# 15. Consult Invoice

## 15.1 Purpose

Recupera una `Invoice` de acuerdo con los permisos del usuario solicitante.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Invoice.
3. Validate Invoice existence.
4. Validate authorization: Buyer dueño del Order relacionado, Admin, o Supervisor.
5. Return Invoice.
```

---

# 16. Void Invoice

## 16.1 Purpose

Anula una `Invoice` previamente emitida según las reglas de negocio aplicables.

## 16.2 Required Validations

* `requestingUser.role == ADMIN` — anular una factura es una decisión administrativa excepcional, nunca autoservicio del buyer.
* `Invoice` existe.
* `Invoice.invoiceStatus != VOIDED` (no se puede anular dos veces).

## 16.3 Domain Behavior

```java
invoice.voidInvoice();
```

## 16.4 Effect on Order

Este servicio no revierte el `Order` asociado — anular una `Invoice` es una operación puramente financiera/documental. Si la anulación implica también reembolsar al buyer, eso se coordina explícitamente con `Returns and Refunds Management` (`Process Refund`), no ocurre automáticamente como efecto secundario de este servicio.

---

# 17. Exception Model

```text
InvoiceNotFoundException
DuplicateInvoiceException
InvalidOrderStatusForInvoicingException
InvoiceAlreadyVoidedException
UnauthorizedInvoiceOperationException
MissingProductPriceException
```

---

# 18. Persistence Boundary

```text
InvoicingManagementService
        |
        v
InvoiceRepositoryPort
        |
        v
InvoicePersistenceAdapter
        |
        v
Database
```

---

# 19. Controller Responsibilities

Los controllers no deben implementar:

* Cálculo de `totalAmount`.
* Reglas de unicidad de factura por pedido.
* Persistencia.

---

# 20. Validation Matrix

| Validation | Issue | Consult | Void |
|---|---:|---:|---:|
| Requesting User | N/A (interno, vía Order) | Yes | Yes (Admin only) |
| Order status (PAID) | Yes | N/A | N/A |
| Invoice uniqueness per Order | Yes | N/A | N/A |
| Invoice existence | N/A (se crea) | Yes | Yes |
| Status transition validity | N/A | N/A | Yes (no re-void) |

---

# 21. Business Rules Summary

## BR-001 — One Invoice per Order

## BR-002 — totalAmount Is Always Calculated, Never Supplied

## BR-003 — Issue Invoice Is Triggered by Order Management, Not Standalone

## BR-004 — Void Is an Administrative Decision

Solo `ADMIN` puede anular una factura.

## BR-005 — Void Does Not Automatically Trigger a Refund

## BR-006 — Domain State Changes Through Domain Behavior

## BR-007 — Services Must Be Cohesive

---

# 22. Anti-Patterns

## 22.1 Direct Database Access

## 22.2 Primitive Application Contracts

## 22.3 Accepting totalAmount from the Caller

Inválido:

```java
issueInvoice(Order order, BigDecimal totalAmount);
```

## 22.4 Issuing a Second Invoice for the Same Order

Inválido: no validar `InvoiceRepositoryPort.findByOrder` antes de crear una nueva `Invoice`.

## 22.5 Void Automatically Triggering a Refund

Inválido: que `voidInvoice()` dispare internamente `ProcessRefundUseCase` sin una decisión explícita del actor administrativo.

---

# 23. Testing Requirements

## 23.1 Issue Invoice Tests

* Emisión válida para un `Order` `PAID`.
* Intento de emitir para un `Order` no `PAID` (rechazado).
* Intento de emitir una segunda factura para el mismo `Order` (rechazado).

## 23.2 Consult Invoice Tests

* Buyer consulta la factura de su propio order.
* Buyer intenta consultar factura de otro buyer (rechazado).

## 23.3 Void Invoice Tests

* Anulación válida por Admin.
* Buyer intenta anular (rechazado).
* Intento de anular una factura ya `VOIDED` (rechazado).

---

# 24. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models apropiados.
* No expone DTOs REST como contratos de aplicación.
* Calcula `totalAmount` internamente, nunca lo recibe como input.
* Garantiza una única `Invoice` por `Order`.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 25. Final Service Catalog

```text
Invoicing Management
|
+-- Issue Invoice
|
+-- Consult Invoice
|
+-- Void Invoice
```

---

# 26. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, calculando `totalAmount` siempre desde el estado autoritativo del Order, garantizando una única Invoice por Order, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**