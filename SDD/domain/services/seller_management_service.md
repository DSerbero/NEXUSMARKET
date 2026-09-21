# Seller Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Seller Management** del sistema NexusMarket.

El subdominio Seller Management es responsable de gestionar el ciclo de vida y las operaciones de negocio asociadas a los sellers.

Las principales capacidades de negocio son:

* Register Seller.
* Consult Seller.
* Update Seller.
* Consult Seller Catalog.

Los sellers están representados por el Domain Model `Seller`, que extiende de `User`.

```text
User
  |
  +-- Seller
```

Un `Seller` no puede autoregistrarse; es incorporado exclusivamente por un `User` con `role = ADMIN` (regla de negocio de la especificación funcional, Dominio 3).

```text
Admin (User)
    |
    | incorpora
    v
Seller
```

Esta relación debe validarse explícitamente cada vez que una operación de creación de `Seller` sea solicitada.

> **Nota de trazabilidad:** el dominio de NexusMarket no define actualmente un Domain Model equivalente a `Operation`/`AuditLog` del ejemplo bancario. Por lo tanto, los servicios de este documento no incluyen pasos de registro de auditoría — si el negocio llegara a requerir trazabilidad histórica de estas operaciones, eso implicaría agregar una nueva entidad al Domain Model, lo cual está fuera del alcance actual y debe tratarse como una decisión de diseño aparte.

---

# 2. Domain Model Context

## 2.1 Seller

`Seller` es un Domain Model que extiende de `User`.

Conceptualmente:

```text
User
  |
  +-- Seller
         |
         +-- associatedWarehouses : List<Warehouse>
         +-- productCatalog : List<Product>
```

Los atributos comunes que pertenecen a `User` (`identifier`, `fullName`, `email`, `role`, `status`) no deben duplicarse en `Seller`.

## 2.2 Seller Relationships

```text
Seller
  |
  +-- 1..* --> Warehouse   (associatedWarehouses)
  |
  +-- 1..* --> Product     (productCatalog)
```

Estas dos relaciones son de solo lectura desde la perspectiva de `Seller Management`: las operaciones que crean o modifican `Warehouse` y `Product` pertenecen a sus respectivos subdominios (`Warehouse Management`, `Product Catalog Management`), no a este.

---

# 3. Admin, User, and Seller Relationship

Las operaciones de Seller Management involucran hasta tres conceptos distintos:

```text
User (role = ADMIN)
 |
 | ejecuta la operación
 v
Seller
 |
 | es en sí mismo un User (role = SELLER)
 v
User
```

Estos conceptos no deben tratarse como intercambiables.

### Requesting User (Admin)

El `User` con `role = ADMIN` representa el actor que solicita o ejecuta la operación de registro/administración del seller.

### Seller

El `Seller` representa al participante de negocio que se está creando, consultando o actualizando. Es en sí mismo una instancia de `User`.

Por lo tanto:

```text
Requesting User != Seller (excepto en operaciones de autoconsulta, ver 3.1)
```

## 3.1 Autoconsulta

Un `Seller` puede consultar y actualizar su propia información sin ser `ADMIN`. En ese caso, `requestingUser == seller` es una condición válida, distinta del caso administrativo. El servicio debe distinguir explícitamente entre ambos escenarios de autorización (delegado al subdominio Authorization).

---

# 4. Service Design Principle

Cada servicio de Seller Management representa una operación de negocio cohesiva.

Un servicio es responsable de determinar y validar **todas las condiciones de negocio necesarias para ejecutar correctamente esa operación**.

La arquitectura no debe fragmentar artificialmente una operación en múltiples servicios pequeños.

Por ejemplo, es aceptable que:

```text
RegisterSellerService
```

realice:

```text
Admin validation
Seller data validation
Email uniqueness validation
Initial Warehouse creation coordination
Persistence
```

a través de métodos privados o colaboradores cohesivos.

No es necesario crear:

```text
ValidateAdminService
ValidateSellerDataService
ValidateEmailUniquenessService
```

solo para separar validaciones.

---

# 5. Standard Application Service Pattern

Los servicios de Seller Management que cambian estado generalmente siguen este patrón:

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative state (cuando aplica)
                |
                v
Validate requesting User (Admin o el propio Seller)
                |
                v
Validate Seller data
                |
                v
Validate business rules (unicidad de email, rol, etc.)
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

No todos los servicios requieren todas las validaciones mostradas. Cada servicio aplica las relevantes a su operación.

---

# 6. Input Contract

Los servicios de aplicación de Seller Management deben operar usando Domain Models y Value Objects. No deben exponer DTOs REST ni entidades de persistencia como contratos de aplicación.

Incorrecto:

```java
registerSeller(
    String fullName,
    String email,
    String warehouseLocation
);
```

Preferido:

```java
registerSeller(
    User requestingAdmin,
    Seller newSeller,
    Warehouse initialWarehouse
);
```

La firma exacta puede variar según el Domain Model del proyecto, pero el principio arquitectónico es obligatorio.

---

# 7. Authoritative State

Los Domain Models recibidos por un servicio representan el contexto de la operación, pero no deben considerarse automáticamente el estado persistido autoritativo.

Para operaciones de consulta y actualización, el servicio debe recuperar el `Seller` actual a través del Output Port correspondiente cuando se requiera el estado actual.

```text
Input Seller
       |
       v
SellerRepositoryPort
       |
       v
Authoritative Seller
       |
       v
Business Validation
```

Esto es particularmente importante para:

* `email` (unicidad).
* `status`.
* `productCatalog` / `associatedWarehouses` actuales.

---

# 8. External Information

La información ya contenida en un Domain Model debe validarse desde ese Domain Model siempre que sea posible.

La información externa requerida para una decisión de negocio debe obtenerse a través de un Output Port.

Ejemplo:

```text
Seller Management Service
        |
        v
UserRepositoryPort
        |
        v
User Adapter
        |
        v
External Persistence
```

Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Admin Validation

Cuando una operación es ejecutada por un `Admin`, el servicio debe validar el `User` solicitante según los requisitos de negocio de esa operación.

Validaciones relevantes pueden incluir:

* El `User` existe.
* El `User` está activo.
* El `User.role == ADMIN`.
* El `User` está autorizado (delegado al subdominio Authorization).

El servicio no debe asumir:

```text
User existe
    =
User está autorizado
```

---

# 10. Seller Validation

Cuando una operación involucra a un `Seller`, el servicio debe validar según los requisitos de negocio:

* Datos obligatorios presentes (`fullName`, `email`).
* `email` único en la plataforma (RG de la especificación: "El correo electrónico debe ser único").
* `Seller.status` es válido para la operación solicitada.

El servicio no debe asumir:

```text
Seller data presente
    =
Seller data válida
```

Cuando se requiera información autoritativa de `Seller`, debe obtenerse a través de:

```text
SellerRepositoryPort
```

---

# 11. Domain Behavior

El estado de un `Seller` debe cambiarse a través de comportamiento de dominio válido, no de setters sin restricciones.

Preferido:

```java
seller.updateContactInformation(fullName, email);
seller.deactivate();
```

Evitar:

```java
seller.setEmail(...);
seller.setStatus(...);
```

El Domain Model debe proteger sus propios invariantes (p. ej., no permitir un `email` vacío).

---

# 12. Input Ports

El subdominio Seller Management expone los siguientes Input Ports:

```text
RegisterSellerUseCase
ConsultSellerUseCase
UpdateSellerUseCase
ConsultSellerCatalogUseCase
```

La validación de que el solicitante es `ADMIN` es responsabilidad de negocio del servicio aplicable (delegada al subdominio Authorization). No necesita exponerse como un caso de uso de aplicación independiente.

---

# 13. Output Ports

El subdominio Seller Management puede usar los siguientes Output Ports:

```text
SellerRepositoryPort
UserRepositoryPort
WarehouseRepositoryPort
ProductRepositoryPort
```

## Service-to-Port Matrix

| Service | SellerRepositoryPort | UserRepositoryPort | WarehouseRepositoryPort | ProductRepositoryPort |
|---|---:|---:|---:|---:|
| Register Seller | ✓ (save) | ✓ (validar unicidad de email) | ✓ (crear warehouse inicial) | |
| Consult Seller | ✓ (read) | | | |
| Update Seller | ✓ (update) | ✓ (cuando aplica) | | |
| Consult Seller Catalog | ✓ (read) | | ✓ (read) | ✓ (read) |

Una celda vacía significa que el servicio no requiere ese Output Port.

---

# 14. SellerRepositoryPort

`SellerRepositoryPort` es responsable de la persistencia y recuperación de `Seller`.

Conceptualmente:

```java
public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findByIdentifier(Seller seller);

    Optional<Seller> findByEmail(Seller seller);

    boolean existsByEmail(Seller seller);

    Seller update(Seller seller);
}
```

Los nombres y firmas exactos pueden variar. El puerto debe operar usando Domain Models.

---

# 15. Register Seller

## 15.1 Purpose

Crea un nuevo `Seller` y su `Warehouse` inicial, incorporándolo al Marketplace.

Restringido al rol `ADMIN`, dado que los sellers no pueden autoregistrarse (Dominio 3 de la especificación).

## 15.2 Input

```text
User (requestingAdmin)
Seller (newSeller)
Warehouse (initialWarehouse)
```

## 15.3 Validations

El servicio debe validar todas las reglas necesarias para crear el seller, incluyendo cuando aplique:

* `requestingAdmin` existe, está activo y tiene `role == ADMIN`.
* `requestingAdmin` está autorizado (Authorization subdomain).
* `newSeller.email` es único en la plataforma.
* `newSeller.fullName` no está vacío.
* `initialWarehouse` contiene la información mínima requerida (`location`).

## 15.4 Domain Creation

```text
Seller
      |
      v
Validate
      |
      v
Create Domain State (role = SELLER, status = ACTIVE)
      |
      v
SellerRepositoryPort.save(seller)
      |
      v
Create initial Warehouse (ownerType = SELLER, owner = seller, responsibleUser = seller)
      |
      v
WarehouseRepositoryPort.save(warehouse)
```

Ambas persistencias deben ejecutarse de forma transaccionalmente consistente: no debe quedar un `Seller` creado sin su `Warehouse` inicial, ni viceversa.

---

# 16. Consult Seller

## 16.1 Purpose

Recupera un `Seller` existente al cual el actor solicitante está autorizado a acceder.

## 16.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Seller.
3. Validate Seller existence.
4. Validate authorization (Admin, Supervisor, o el propio Seller).
5. Return Seller.
```

## 16.3 Persistence

El seller debe recuperarse a través de:

```text
SellerRepositoryPort
```

Las entidades de persistencia nunca deben retornarse fuera del adaptador de persistencia.

---

# 17. Update Seller

## 17.1 Purpose

Actualiza la información mantenida para un `Seller` existente.

## 17.2 Required Validations

* `requestingUser` es `ADMIN` o el propio `Seller`.
* `Seller` existe.
* Si se actualiza `email`, el nuevo valor es único en la plataforma.
* `fullName` no queda vacío.

## 17.3 Domain Behavior

```java
seller.updateContactInformation(fullName, email);
```

## 17.4 Persistence

```text
SellerRepositoryPort.update(seller)
```

---

# 18. Consult Seller Catalog

## 18.1 Purpose

Recupera los `Product` y `Warehouse` asociados a un `Seller`.

## 18.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Seller.
3. Validate Seller existence.
4. Validate authorization (Admin, Supervisor, o el propio Seller).
5. Retrieve productCatalog via ProductRepositoryPort.
6. Retrieve associatedWarehouses via WarehouseRepositoryPort.
7. Return combined result.
```

Este servicio es de solo lectura; no modifica estado del `Seller`, `Product` ni `Warehouse`.

---

# 19. Exception Model

El subdominio Seller Management puede usar excepciones como:

```text
SellerNotFoundException
InvalidSellerDataException
DuplicateSellerEmailException
UnauthorizedSellerRegistrationException
UnauthorizedSellerOperationException
InvalidUserStatusException
```

Las excepciones deben comunicar claramente fallos de negocio. La implementación no debe depender de excepciones técnicas (`NullPointerException`, `SQLException`, etc.) para representar violaciones de reglas de dominio.

---

# 20. Persistence Boundary

```text
Application Service
        |
        v
Output Port
        |
        v
Persistence Adapter
        |
        v
Database
```

Para Seller:

```text
SellerManagementService
        |
        v
SellerRepositoryPort
        |
        v
SellerPersistenceAdapter
        |
        v
Database
```

---

# 21. Controller Responsibilities

Los controllers son responsables únicamente de aspectos de transporte. Pueden:

1. Recibir requests externos.
2. Validar a nivel de transporte.
3. Mapear datos de request a Domain Models y Value Objects.
4. Invocar un Input Port.
5. Mapear el resultado de dominio a la respuesta externa.

Los controllers no deben implementar:

* Validación de unicidad de email.
* Reglas de creación de warehouse inicial.
* Reglas de autorización.
* Persistencia.

---

# 22. Validation Matrix

| Validation | Register | Consult | Update | Consult Catalog |
|---|---:|---:|---:|---:|
| Requesting User | Yes | Yes | Yes | Yes |
| User status | Yes | Yes | Yes | Yes |
| Authorization | Yes | Yes | Yes | Yes |
| Seller existence | N/A | Yes | Yes | Yes |
| Email uniqueness | Yes | N/A | When email changes | N/A |
| Initial Warehouse validity | Yes | N/A | N/A | N/A |

---

# 23. Business Rules Summary

## BR-001 — Seller extends User

```text
User
  |
  +-- Seller
```

## BR-002 — Sellers Cannot Self-Register

Solo un `User` con `role == ADMIN` puede ejecutar `Register Seller`.

## BR-003 — Email Must Be Unique

`Seller.email` (heredado de `User.email`) debe ser único en toda la plataforma.

## BR-004 — Seller Owns Its Catalog and Warehouses

```text
Seller.productCatalog : List<Product>
Seller.associatedWarehouses : List<Warehouse>
```

Estas relaciones son de solo lectura desde Seller Management.

## BR-005 — Domain State Changes Through Domain Behavior

El servicio de aplicación debe usar comportamiento de dominio válido en vez de setters sin restricciones.

## BR-006 — External Information Uses Output Ports

El servicio de aplicación nunca debe acceder directamente a bases de datos o infraestructura.

## BR-007 — Services Must Be Cohesive

Un servicio de aplicación puede realizar todas las validaciones requeridas para su operación de negocio. La arquitectura no debe fragmentarse solo por crear servicios más pequeños.

---

# 24. Anti-Patterns

## 24.1 Direct Database Access

Inválido: `EntityManager`, `JpaRepository`, `JdbcTemplate` dentro de servicios de aplicación.

## 24.2 Primitive Application Contracts

Evitar:

```java
registerSeller(
    String fullName,
    String email
);
```

## 24.3 Trusting Caller-Supplied State

No asumir que el `Seller` suministrado por el caller contiene el `email` o `status` persistido actual.

## 24.4 Missing Admin Validation

Inválido:

```text
Seller data válida
=
Seller puede registrarse
```

La validación de que el solicitante es `ADMIN` debe establecerse explícitamente.

## 24.5 Business Rules in Controllers

Inválido:

```text
Controller
 |
 +-- check email uniqueness
 +-- create warehouse
 +-- persist seller
```

---

# 25. Testing Requirements

El subdominio Seller Management debe ser testeable sin infraestructura. Los servicios de aplicación deben ser testeables usando mocks, fakes o stubs para los Output Ports.

## 25.1 Register Seller Tests

* Admin válido registra seller exitosamente.
* Usuario no-Admin intenta registrar seller (rechazado).
* Email duplicado (rechazado).
* Datos de seller inválidos (rechazado).
* Warehouse inicial inválida (rechazado).
* Falla de creación de warehouse revierte la creación del seller (consistencia transaccional).

## 25.2 Consult Seller Tests

* Seller existente, usuario autorizado.
* Seller inexistente.
* Usuario no autorizado.

## 25.3 Update Seller Tests

* Actualización válida por Admin.
* Actualización válida por el propio Seller.
* Email duplicado al actualizar (rechazado).
* Usuario no autorizado.

## 25.4 Consult Seller Catalog Tests

* Catálogo recuperado exitosamente.
* Seller sin productos ni warehouses (lista vacía, no error).
* Usuario no autorizado.

---

# 26. Definition of Done

Un servicio de Seller Management está completo únicamente cuando:

* Representa una operación de negocio coherente.
* Recibe los Domain Models y Value Objects apropiados.
* No expone DTOs REST como contratos de aplicación.
* No recibe entidades de persistencia.
* No depende directamente de tecnología de persistencia.
* Recupera estado autoritativo cuando se requiere.
* Valida al usuario solicitante y su rol.
* Valida los datos del Seller según las reglas de negocio.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 27. Final Service Catalog

```text
Seller Management
|
+-- Register Seller
|
+-- Consult Seller
|
+-- Update Seller
|
+-- Consult Seller Catalog
```

---

# 28. Final Design Rule

El subdominio Seller Management debe seguir este principio:

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando todas las condiciones de negocio requeridas de usuario, autorización, unicidad y estado del Seller, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**