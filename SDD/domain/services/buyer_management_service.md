# Buyer Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Buyer Management** del sistema NexusMarket.

El subdominio Buyer Management es responsable de gestionar el ciclo de vida y la información específica de los buyers para su participación en los procesos comerciales.

Las principales capacidades de negocio son:

* Register Buyer.
* Consult Buyer.
* Update Buyer.
* Change Buyer Commercial Status.

Los buyers están representados por el Domain Model `Buyer`, que extiende de `User`.

```text
User
  |
  +-- Buyer
```

A diferencia de `Seller`, un `Buyer` **sí puede autoregistrarse** — la especificación funcional lista "Registro de compradores" como un proceso incluido en el alcance, sin restringirlo a un rol administrativo (a diferencia del Dominio 3, "Gestión de Vendedores", que sí exige incorporación por Admin).

```text
Buyer
    |
    | se autoregistra
    v
Buyer
```

**Restricción clave (Dominio 2 de la especificación):** un buyer nunca administrará información de otros buyers ni datos de inventario. Toda operación de este subdominio debe respetar ese límite.

---

# 2. Domain Model Context

## 2.1 Buyer

`Buyer` es un Domain Model que extiende de `User`.

Conceptualmente:

```text
User
  |
  +-- Buyer
        |
        +-- primaryAddress : String
        +-- additionalAddresses : List<String>
        +-- buyerStatus : BuyerStatus
```

Los atributos comunes que pertenecen a `User` (`identifier`, `fullName`, `email`, `role`, `status`) no deben duplicarse en `Buyer`.

## 2.2 Buyer Relationships

```text
Buyer
  |
  +-- 1..* --> Cart / Order / Return / Refund
```

Estas relaciones son de solo lectura desde la perspectiva de Buyer Management: las operaciones sobre `Cart`, `Order`, `Return` y `Refund` pertenecen a sus respectivos subdominios.

---

# 3. Buyer and Requesting User Relationship

A diferencia de Seller Management (donde el registro requiere un `Admin` distinto del `Seller` creado), en Buyer Management el caso más común es la autogestión:

```text
Requesting User == Buyer  (autoregistro, autoconsulta, autoactualización)
```

Sin embargo, un `Admin` o `Supervisor` también puede consultar información de un `Buyer` dentro de su alcance (por ejemplo, para soporte o reportes administrativos). El servicio debe distinguir explícitamente entre:

```text
Requesting User == Buyer            -> autogestión
Requesting User.role == ADMIN       -> gestión administrativa
Requesting User.role == SUPERVISOR  -> solo lectura
```

Un `Buyer` nunca debe poder operar sobre la información de otro `Buyer`, sin excepción (RG-03 de la especificación).

---

# 4. Service Design Principle

Cada servicio de Buyer Management representa una operación de negocio cohesiva, responsable de validar todas las condiciones necesarias para ejecutarla correctamente, sin fragmentarse artificialmente en servicios más pequeños (mismo principio aplicado en Authorization y Seller Management).

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative state (cuando aplica)
                |
                v
Validate requesting User (el propio Buyer o Admin/Supervisor)
                |
                v
Validate Buyer data
                |
                v
Validate business rules (unicidad de email, formato de direcciones, etc.)
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
registerBuyer(
    String fullName,
    String email,
    String primaryAddress
);
```

Preferido:

```java
registerBuyer(
    Buyer newBuyer
);
```

---

# 7. Authoritative State

```text
Input Buyer
       |
       v
BuyerRepositoryPort
       |
       v
Authoritative Buyer
       |
       v
Business Validation
```

Particularmente importante para `email` (unicidad) y `buyerStatus`.

---

# 8. External Information

La información externa requerida para una decisión de negocio debe obtenerse a través de un Output Port (`BuyerRepositoryPort`). Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Buyer Validation

El servicio debe validar, según los requisitos de negocio:

* Datos obligatorios presentes (`fullName`, `email`, `primaryAddress`).
* `email` único en la plataforma.
* `primaryAddress` no vacío (obligatorio según la especificación; `additionalAddresses` es opcional).
* `buyerStatus` es válido para la operación solicitada (por ejemplo, un buyer `SUSPENDED` no debería poder actualizar su información comercial en ciertos flujos, según la regla de negocio que se defina).

---

# 10. Domain Behavior

Preferido:

```java
buyer.updateContactInformation(fullName, email);
buyer.updateAddresses(primaryAddress, additionalAddresses);
buyer.suspend();
buyer.enable();
```

Evitar:

```java
buyer.setEmail(...);
buyer.setBuyerStatus(...);
```

---

# 11. Input Ports

```text
RegisterBuyerUseCase
ConsultBuyerUseCase
UpdateBuyerUseCase
ChangeBuyerCommercialStatusUseCase
```

---

# 12. Output Ports

```text
BuyerRepositoryPort
```

## Service-to-Port Matrix

| Service | BuyerRepositoryPort |
|---|---:|
| Register Buyer | ✓ (save) |
| Consult Buyer | ✓ (read) |
| Update Buyer | ✓ (update) |
| Change Buyer Commercial Status | ✓ (update) |

---

# 13. BuyerRepositoryPort

`BuyerRepositoryPort` es responsable de la persistencia y recuperación de `Buyer`.

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

# 14. Register Buyer

## 14.1 Purpose

Crea un nuevo `Buyer` y establece su estado comercial inicial e información de entrega.

## 14.2 Input

```text
Buyer (newBuyer)
```

## 14.3 Validations

* `newBuyer.email` es único en la plataforma.
* `newBuyer.fullName` no está vacío.
* `newBuyer.primaryAddress` no está vacío.
* `newBuyer.buyerStatus` se inicializa como `ENABLED` por defecto — no debe ser un valor arbitrario suministrado por el caller.

## 14.4 Domain Creation

```text
Buyer
      |
      v
Validate
      |
      v
Create Domain State (role = BUYER, status = ACTIVE, buyerStatus = ENABLED)
      |
      v
BuyerRepositoryPort.save(buyer)
```

---

# 15. Consult Buyer

## 15.1 Purpose

Recupera un `Buyer` existente al cual el actor solicitante está autorizado a acceder.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Buyer.
3. Validate Buyer existence.
4. Validate authorization: requestingUser == buyer, o requestingUser.role in {ADMIN, SUPERVISOR}.
5. Return Buyer.
```

Un `Buyer` que intente consultar la información de otro `Buyer` debe ser rechazado, sin excepción (RG-03).

---

# 16. Update Buyer

## 16.1 Purpose

Actualiza la información mantenida para un `Buyer` existente, incluyendo sus direcciones de entrega.

## 16.2 Required Validations

* `requestingUser` es el propio `Buyer` o `ADMIN`.
* `Buyer` existe.
* Si se actualiza `email`, el nuevo valor es único en la plataforma.
* `primaryAddress` no queda vacío tras la actualización.

## 16.3 Domain Behavior

```java
buyer.updateContactInformation(fullName, email);
buyer.updateAddresses(primaryAddress, additionalAddresses);
```

## 16.4 Persistence

```text
BuyerRepositoryPort.update(buyer)
```

---

# 17. Change Buyer Commercial Status

## 17.1 Purpose

Cambia el estado comercial de un `Buyer`, habilitándolo o suspendiéndolo para realizar compras.

## 17.2 Required Validations

* `requestingUser.role == ADMIN` (un buyer no puede autosuspenderse ni autohabilitarse — es una decisión administrativa).
* `Buyer` existe.
* La transición de `buyerStatus` es válida.

## 17.3 Default Transitions

```text
ENABLED   -> SUSPENDED
SUSPENDED -> ENABLED
```

## 17.4 Domain Behavior

```java
buyer.suspend();
buyer.enable();
```

## 17.5 Effect on Other Subdominios

Un `Buyer` con `buyerStatus == SUSPENDED` no debe poder confirmar un `Cart` ni crear un `Order` nuevo — esa validación pertenece a los subdominios `Cart Management` y `Order Management` respectivamente, que deben consultar `Buyer.buyerStatus` como parte de su propia validación de negocio.

---

# 18. Exception Model

```text
BuyerNotFoundException
InvalidBuyerDataException
DuplicateBuyerEmailException
UnauthorizedBuyerOperationException
InvalidBuyerStatusTransitionException
InvalidUserStatusException
```

---

# 19. Persistence Boundary

```text
BuyerManagementService
        |
        v
BuyerRepositoryPort
        |
        v
BuyerPersistenceAdapter
        |
        v
Database
```

---

# 20. Controller Responsibilities

Los controllers no deben implementar:

* Validación de unicidad de email.
* Reglas de transición de `buyerStatus`.
* Reglas de autorización.
* Persistencia.

---

# 21. Validation Matrix

| Validation | Register | Consult | Update | Change Commercial Status |
|---|---:|---:|---:|---:|
| Requesting User | N/A (autoregistro) | Yes | Yes | Yes |
| User status | N/A | Yes | Yes | Yes |
| Authorization | N/A | Yes (self/Admin/Supervisor) | Yes (self/Admin) | Yes (Admin only) |
| Buyer existence | N/A | Yes | Yes | Yes |
| Email uniqueness | Yes | N/A | When email changes | N/A |
| Address validity | Yes | N/A | Yes | N/A |
| Status transition validity | N/A | N/A | N/A | Yes |

---

# 22. Business Rules Summary

## BR-001 — Buyer Extends User

```text
User
  |
  +-- Buyer
```

## BR-002 — Buyers Can Self-Register

A diferencia de `Seller`, no se requiere un `Admin` para crear un `Buyer`.

## BR-003 — Email Must Be Unique

`Buyer.email` (heredado de `User.email`) debe ser único en toda la plataforma.

## BR-004 — Buyers Never Access Other Buyers' Information

Regla de negocio explícita de la especificación (Dominio 2, RG-03).

## BR-005 — Commercial Status Changes Are Administrative

Solo `ADMIN` puede ejecutar `Change Buyer Commercial Status`; un buyer no puede autosuspenderse.

## BR-006 — Domain State Changes Through Domain Behavior

## BR-007 — External Information Uses Output Ports

## BR-008 — Services Must Be Cohesive

---

# 23. Anti-Patterns

## 23.1 Direct Database Access

## 23.2 Primitive Application Contracts

## 23.3 Trusting Caller-Supplied State

No asumir que el `buyerStatus` suministrado por el caller es el estado persistido actual.

## 23.4 Missing Ownership Validation

Inválido:

```text
Buyer existe
=
Requesting User puede consultarlo
```

Debe validarse explícitamente que `requestingUser == buyer` o que el solicitante tiene un rol administrativo.

## 23.5 Self-Service Status Changes

Inválido: permitir que un `Buyer` cambie su propio `buyerStatus`. Esa es una decisión administrativa (`ADMIN`).

---

# 24. Testing Requirements

## 24.1 Register Buyer Tests

* Registro válido.
* Email duplicado (rechazado).
* `primaryAddress` vacío (rechazado).
* `buyerStatus` inicial siempre `ENABLED`, sin importar lo que envíe el caller.

## 24.2 Consult Buyer Tests

* Buyer consulta su propia información (permitido).
* Buyer intenta consultar información de otro buyer (rechazado).
* Admin/Supervisor consulta cualquier buyer (permitido).

## 24.3 Update Buyer Tests

* Actualización válida por el propio buyer.
* Email duplicado al actualizar (rechazado).
* `primaryAddress` vacío tras actualización (rechazado).

## 24.4 Change Buyer Commercial Status Tests

* Admin suspende un buyer `ENABLED` (permitido).
* Admin habilita un buyer `SUSPENDED` (permitido).
* Buyer intenta cambiar su propio estado (rechazado).
* Transición inválida, p. ej. `SUSPENDED -> SUSPENDED` (rechazada).

---

# 25. Definition of Done

Un servicio de Buyer Management está completo únicamente cuando:

* Representa una operación de negocio coherente.
* Recibe los Domain Models y Value Objects apropiados.
* No expone DTOs REST como contratos de aplicación.
* Recupera estado autoritativo cuando se requiere.
* Valida al usuario solicitante y distingue correctamente autogestión vs. gestión administrativa.
* Valida los datos del Buyer según las reglas de negocio.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 26. Final Service Catalog

```text
Buyer Management
|
+-- Register Buyer
|
+-- Consult Buyer
|
+-- Update Buyer
|
+-- Change Buyer Commercial Status
```

---

# 27. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando todas las condiciones requeridas de usuario, autorización, unicidad, validez de dirección y estado comercial del Buyer, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**