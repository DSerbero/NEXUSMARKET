# Warehouse Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Warehouse Management** del sistema NexusMarket.

El subdominio Warehouse Management es responsable de controlar los espacios físicos de almacenamiento del Marketplace.

Las principales capacidades de negocio son:

* Register Warehouse.
* Consult Warehouse.
* Assign Logistics Operator.
* Update Warehouse.

Las warehouses están representadas por el Domain Model `Warehouse`, una clase concreta única (sin subclases), según la decisión de diseño ya tomada: la distinción entre una warehouse del Marketplace y una warehouse de un seller no implica atributos ni comportamiento distintos, por lo que se modela como un atributo (`ownerType`) en vez de como jerarquía de clases.

```text
Warehouse
  |
  +-- ownerType : WarehouseOwnerType   (MARKETPLACE | SELLER)
  +-- owner : User                     (Admin o Seller)
  +-- responsibleUser : User           (normalmente un LogisticsOperator)
```

> **Relación con Seller Management:** la primera `Warehouse` de un `Seller` se crea como parte de `Register Seller` (ver Seller Management Services, §15), no de este subdominio. Los servicios de este documento gestionan warehouses **adicionales** a esa primera warehouse, y las warehouses propias del Marketplace (`ownerType == MARKETPLACE`).

---

# 2. Domain Model Context

## 2.1 Warehouse

```text
Warehouse
  |
  +-- identifier
  +-- location
  +-- ownerType : WarehouseOwnerType
  +-- owner : User
  +-- responsibleUser : User
```

## 2.2 Owner vs. Responsible User

Estos dos conceptos no deben confundirse, y la relación entre ambos **depende de `ownerType`**:

```text
MARKETPLACE  -> owner = Admin, responsibleUser = LogisticsOperator (asignado por separado)
SELLER       -> owner = Seller, responsibleUser = ese mismo Seller (no hay operador logístico distinto)
```

Es decir:

```text
ownerType == MARKETPLACE  =>  responsibleUser puede ser distinto de owner (un LogisticsOperator)
ownerType == SELLER       =>  responsibleUser siempre es igual a owner (el propio Seller)
```

La especificación funcional describe al Operador Logístico como responsable de "la operación física de bodegas y despachos" en general, pero solo las warehouses del Marketplace requieren esa asignación explícita — la bodega de un seller está, de negocio, a cargo del propio seller, no de un operador logístico asignado.

---

# 3. Requesting User, Owner, and Responsible User Relationship

Las operaciones de Warehouse Management involucran hasta tres actores:

```text
Requesting User
 |
 | ejecuta la operación
 v
Warehouse.owner        (Admin o Seller — dueño)
 |
 v
Warehouse.responsibleUser   (LogisticsOperator — operador)
```

* `ADMIN` puede ejecutar cualquier operación sobre cualquier `Warehouse`, sin restricción de propiedad.
* `SELLER` solo puede ejecutar operaciones sobre warehouses donde `Warehouse.owner == requestingUser`. Para sus propias warehouses, el `SELLER` es también el `responsibleUser` — no existe un `LogisticsOperator` intermediario.
* `LOGISTICS_OPERATOR` solo puede consultar/actualizar información operativa de warehouses **`MARKETPLACE`** donde `Warehouse.responsibleUser == requestingUser`. Un `LogisticsOperator` nunca es `responsibleUser` de una warehouse `SELLER`.

---

# 4. Service Design Principle

Cada servicio de Warehouse Management representa una operación de negocio cohesiva, responsable de validar todas las condiciones necesarias para ejecutarla correctamente.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative state
                |
                v
Validate requesting User
                |
                v
Validate ownership (Admin sin restricción, Seller solo su propia warehouse)
                |
                v
Validate Warehouse data
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
registerWarehouse(
    String location,
    String ownerId,
    String ownerType
);
```

Preferido:

```java
registerWarehouse(
    User requestingUser,
    Warehouse newWarehouse
);
```

`ownerType` debe representarse como el Value Object `WarehouseOwnerType`, nunca como un `String` crudo.

---

# 7. Authoritative State

```text
Input Warehouse
       |
       v
WarehouseRepositoryPort
       |
       v
Authoritative Warehouse
       |
       v
Business Validation
```

Particularmente importante para `owner`, `ownerType` y `responsibleUser` — el servicio nunca debe confiar en estos valores tal como los envía el caller cuando se requiere el estado actual persistido.

---

# 8. External Information

Cuando se requiere validar que el `owner` suministrado corresponde a un `Seller` o `Admin` real y activo, el servicio debe usar `UserRepositoryPort` (o `SellerRepositoryPort` cuando el owner es un seller). Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Warehouse Validation

* `location` no está vacía.
* `ownerType` es un valor válido del catálogo (`MARKETPLACE` o `SELLER`).
* Si `ownerType == MARKETPLACE`, entonces `owner.role == ADMIN`, y `responsibleUser` (cuando se asigna) debe tener `role == LOGISTICS_OPERATOR`.
* Si `ownerType == SELLER`, entonces `owner` corresponde a un `Seller` existente, y `responsibleUser` debe ser igual a `owner` — nunca un `LogisticsOperator` independiente.

La primera de estas dos ramas depende de una validación de negocio explícita del servicio: nada en el tipo de `responsibleUser` (`User`) impide, por sí solo, asignar un `Buyer` como responsable de una warehouse `MARKETPLACE`, ni asignar un `LogisticsOperator` a una warehouse `SELLER` donde no corresponde.

---

# 10. Domain Behavior

Preferido:

```java
warehouse.updateLocation(newLocation);
warehouse.assignResponsibleUser(logisticsOperator);
```

Evitar:

```java
warehouse.setLocation(...);
warehouse.setResponsibleUser(...);
```

---

# 11. Input Ports

```text
RegisterWarehouseUseCase
ConsultWarehouseUseCase
AssignLogisticsOperatorUseCase
UpdateWarehouseUseCase
```

---

# 12. Output Ports

```text
WarehouseRepositoryPort
UserRepositoryPort
SellerRepositoryPort
```

## Service-to-Port Matrix

| Service | WarehouseRepositoryPort | UserRepositoryPort | SellerRepositoryPort |
|---|---:|---:|---:|
| Register Warehouse | ✓ (save) | ✓ (validar owner Admin) | ✓ (validar owner Seller) |
| Consult Warehouse | ✓ (read) | | |
| Assign Logistics Operator | ✓ (update) | ✓ (validar rol LOGISTICS_OPERATOR) | |
| Update Warehouse | ✓ (update) | | |

---

# 13. WarehouseRepositoryPort

```java
public interface WarehouseRepositoryPort {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findByIdentifier(Warehouse warehouse);

    List<Warehouse> findByOwner(Warehouse warehouse);

    Warehouse update(Warehouse warehouse);
}
```

`findByOwner` es una operación orientada a dominio, usada por servicios como `Consult Seller Catalog` (Seller Management) y por validaciones de propiedad en Authorization.

---

# 14. Register Warehouse

## 14.1 Purpose

Crea una nueva `Warehouse` y la asocia al Marketplace o a un seller.

## 14.2 Input

```text
User (requestingUser)
Warehouse (newWarehouse)
```

## 14.3 Validations

* `requestingUser.role == ADMIN` (para `ownerType == MARKETPLACE`) o `requestingUser.role == SELLER` (para `ownerType == SELLER`, creando una warehouse adicional propia).
* `newWarehouse.location` no vacía.
* `newWarehouse.ownerType` coincide con el rol del `requestingUser` según la regla anterior.
* Un `SELLER` no puede crear una warehouse con `ownerType == MARKETPLACE`, ni asignar `owner` a otro seller.

## 14.4 Domain Creation

```text
Warehouse
      |
      v
Validate
      |
      v
Create Domain State
      |
      ├── ownerType == MARKETPLACE  -> owner = requestingUser (Admin); responsibleUser = null hasta asignación (ver §16)
      |
      └── ownerType == SELLER       -> owner = requestingUser (Seller); responsibleUser = requestingUser (mismo Seller)
      |
      v
WarehouseRepositoryPort.save(warehouse)
```

Para warehouses `SELLER`, `responsibleUser` se establece automáticamente como el propio seller en el momento de la creación — no requiere ni admite una asignación posterior a través de `Assign Logistics Operator` (§16).

---

# 15. Consult Warehouse

## 15.1 Purpose

Recupera una `Warehouse` existente a la cual el actor solicitante está autorizado a acceder.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Warehouse.
3. Validate Warehouse existence.
4. Validate authorization: ADMIN (sin restricción), SELLER (solo si owner == requestingUser),
   LOGISTICS_OPERATOR (solo si responsibleUser == requestingUser), SUPERVISOR (solo lectura, sin restricción).
5. Return Warehouse.
```

---

# 16. Assign Logistics Operator

## 16.1 Purpose

Asigna un `LogisticsOperator` como responsable de la operación diaria de una `Warehouse` **del Marketplace**.

## 16.2 Scope Restriction

Este servicio aplica exclusivamente a warehouses con `ownerType == MARKETPLACE`.

Una warehouse con `ownerType == SELLER` no admite esta operación: su `responsibleUser` es siempre el propio `Seller` dueño, establecido automáticamente en `Register Warehouse` (§14) y en `Register Seller` (Seller Management, §15). Intentar asignar un `LogisticsOperator` a una warehouse `SELLER` debe rechazarse explícitamente, no ignorarse en silencio.

## 16.3 Required Validations

* `requestingUser.role == ADMIN`.
* `Warehouse` existe.
* `Warehouse.ownerType == MARKETPLACE` (de lo contrario, rechazar con `InvalidWarehouseOwnershipForAssignmentException`).
* El `User` a asignar como `responsibleUser` tiene `role == LOGISTICS_OPERATOR`.
* El `LogisticsOperator` está `ACTIVE`.

## 16.4 Domain Behavior

```java
warehouse.assignResponsibleUser(logisticsOperator);
```

## 16.5 Persistence

```text
WarehouseRepositoryPort.update(warehouse)
```

## 16.6 Reassignment

Este mismo servicio cubre tanto la asignación inicial como la reasignación de operador para warehouses `MARKETPLACE` — no se modela como una operación separada, ya que la validación y el comportamiento de dominio son idénticos.

---

# 17. Update Warehouse

## 17.1 Purpose

Actualiza la información mantenida para una `Warehouse` existente (por ejemplo, su `location`).

## 17.2 Required Validations

* `requestingUser.role == ADMIN` o `requestingUser == Warehouse.owner`.
* `Warehouse` existe.
* `location` no queda vacía.

## 17.3 Scope Restriction

Este servicio **no** permite cambiar `owner` ni `ownerType` — cambiar de dueño a una warehouse ya existente no está contemplado en la especificación funcional y representaría una operación de negocio distinta (transferencia de propiedad), fuera del alcance actual. Si se requiere en el futuro, debe definirse como un servicio explícito y separado, no como parte de una actualización genérica.

## 17.4 Domain Behavior

```java
warehouse.updateLocation(newLocation);
```

## 17.5 Persistence

```text
WarehouseRepositoryPort.update(warehouse)
```

---

# 18. Exception Model

```text
WarehouseNotFoundException
InvalidWarehouseDataException
InvalidWarehouseOwnerException
InvalidResponsibleUserRoleException
UnauthorizedWarehouseOperationException
InvalidUserStatusException
```

---

# 19. Persistence Boundary

```text
WarehouseManagementService
        |
        v
WarehouseRepositoryPort
        |
        v
WarehousePersistenceAdapter
        |
        v
Database
```

---

# 20. Controller Responsibilities

Los controllers no deben implementar:

* Validación de que el `ownerType` coincide con el rol del solicitante.
* Validación de que el `responsibleUser` tiene el rol correcto.
* Reglas de autorización.
* Persistencia.

---

# 21. Validation Matrix

| Validation | Register | Consult | Assign Operator | Update |
|---|---:|---:|---:|---:|
| Requesting User | Yes | Yes | Yes | Yes |
| User status | Yes | Yes | Yes | Yes |
| Authorization | Yes (Admin/Seller) | Yes (Admin/Seller/Operator/Supervisor) | Yes (Admin only) | Yes (Admin/Seller-owner) |
| Warehouse existence | N/A | Yes | Yes | Yes |
| ownerType consistency | Yes | N/A | Yes (must be MARKETPLACE) | N/A |
| responsibleUser role validity | Yes (SELLER → owner) | N/A | Yes (LOGISTICS_OPERATOR) | N/A |

---

# 22. Business Rules Summary

## BR-001 — Warehouse Is a Single Concrete Class

No existen subclases `MarketplaceWarehouse` / `SellerWarehouse`; la distinción es el atributo `ownerType`.

## BR-002 — Owner Determines Creation Authorization

Solo `ADMIN` crea warehouses `MARKETPLACE`; solo el `SELLER` dueño crea warehouses `SELLER` adicionales.

## BR-003 — Responsible User Depends on Owner Type

Para `ownerType == MARKETPLACE`, `Warehouse.responsibleUser` debe tener `role == LOGISTICS_OPERATOR`, asignado por un `ADMIN`. Para `ownerType == SELLER`, `Warehouse.responsibleUser` es siempre el propio `Seller` dueño — no se asigna un operador logístico independiente. Ninguna de las dos reglas se garantiza por el tipo de `User`; ambas son validaciones de negocio explícitas.

## BR-004 — Ownership Transfer Is Out of Scope

`Update Warehouse` no permite cambiar `owner` ni `ownerType`.

## BR-005 — Domain State Changes Through Domain Behavior

## BR-006 — External Information Uses Output Ports

## BR-007 — Services Must Be Cohesive

---

# 23. Anti-Patterns

## 23.1 Direct Database Access

## 23.2 Primitive Application Contracts

## 23.3 Trusting Caller-Supplied Ownership

No asumir que `Warehouse.owner` o `ownerType` suministrados por el caller son los valores persistidos actuales.

## 23.4 Assigning the Wrong Kind of Responsible User

Inválido:

```text
User existe
=
User puede ser responsibleUser de una Warehouse
```

Debe validarse explícitamente según `ownerType`: `role == LOGISTICS_OPERATOR` para warehouses `MARKETPLACE`, o `responsibleUser == owner` (el propio Seller) para warehouses `SELLER`. Asignar un `LogisticsOperator` a una warehouse `SELLER` es tan inválido como asignar un `Buyer` a una warehouse `MARKETPLACE`.

## 23.5 Allowing Ownership Changes Through a Generic Update

Inválido: permitir que `Update Warehouse` modifique `owner`/`ownerType` como cualquier otro campo.

---

# 24. Testing Requirements

## 24.1 Register Warehouse Tests

* Admin registra warehouse `MARKETPLACE` (permitido).
* Seller registra warehouse `SELLER` propia (permitido).
* Seller intenta registrar warehouse `MARKETPLACE` (rechazado).
* Seller intenta registrar warehouse para otro seller (rechazado).
* `location` vacía (rechazado).

## 24.2 Consult Warehouse Tests

* Admin consulta cualquier warehouse (permitido).
* Seller consulta su propia warehouse (permitido).
* Seller consulta warehouse de otro seller (rechazado).
* LogisticsOperator consulta su warehouse asignada (permitido).
* LogisticsOperator consulta warehouse no asignada (rechazado).

## 24.3 Assign Logistics Operator Tests

* Asignación válida sobre warehouse `MARKETPLACE`.
* Reasignación válida (reemplaza al operador anterior) sobre warehouse `MARKETPLACE`.
* Intento de asignar operador a una warehouse `SELLER` (rechazado).
* Usuario asignado no tiene rol `LOGISTICS_OPERATOR` (rechazado).
* Usuario asignado está `BLOCKED`/`INACTIVE` (rechazado).
* Seller (o cualquier no-Admin) intenta asignar operador (rechazado).

## 24.4 Update Warehouse Tests

* Actualización válida de `location`.
* Intento de modificar `owner`/`ownerType` a través de este servicio (rechazado o ignorado según el contrato definido).

---

# 25. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models y Value Objects apropiados (`WarehouseOwnerType` como Value Object, no `String`).
* No expone DTOs REST como contratos de aplicación.
* Recupera estado autoritativo cuando se requiere.
* Valida al usuario solicitante, su rol, y la relación de propiedad/asignación.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 26. Final Service Catalog

```text
Warehouse Management
|
+-- Register Warehouse
|
+-- Consult Warehouse
|
+-- Assign Logistics Operator
|
+-- Update Warehouse
```

---

# 27. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando todas las condiciones requeridas de usuario, propiedad (owner), asignación operativa (responsibleUser) y datos de la Warehouse, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**