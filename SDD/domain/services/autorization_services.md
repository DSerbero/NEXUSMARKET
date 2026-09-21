# Authorization Services

## Introduction

Este documento define los servicios responsables de la **autorización** dentro del sistema NexusMarket.

La autorización determina si un `User` autenticado tiene permiso para ejecutar una operación de negocio específica, según su `UserRole` y las reglas de negocio asociadas a esa operación.

Autenticación y autorización son responsabilidades separadas:

```text
Authentication
     │
     ▼
¿Quién es el User?
     │
     ▼
Authorization
     │
     ▼
¿Qué puede hacer el User?
```

La autenticación valida credenciales y establece la identidad del usuario.

La autorización determina si ese usuario autenticado tiene permiso para ejecutar la operación solicitada.

Los servicios de autorización no ejecutan la operación de negocio en sí. Solo determinan si el `User` actual puede iniciarla.

---

# Domain Model Context

La autorización se basa principalmente en los siguientes Domain Models:

```text
User
UserRole
Order
Product
Inventory
Warehouse
Shipment
Return
Refund
```

El Domain Model `User` contiene:

```text
User
├── identifier
├── fullName
├── email
├── role : UserRole
└── status : UserStatus
```

El rol de autorización del usuario está representado por:

```text
User.role : UserRole
```

El servicio debe operar sobre el Domain Model `User`, no sobre un rol o identificador primitivo recibido de forma independiente.

> **Nota de dominio:** a diferencia de un dominio bancario, NexusMarket no define una entidad genérica equivalente a `BankingProduct` u `Operation`. Cada objeto protegido (`Order`, `Product`, `Inventory`, `Warehouse`, `Shipment`, `Return`, `Refund`) se autoriza directamente como su propio Domain Model, sin forzar una superclase común artificial — en línea con la decisión ya tomada de no crear jerarquías de clases que no aporten atributos o comportamiento distintos.

---

# Authorization Principles

## User as Domain Model

Los servicios de autorización deben recibir un Domain Model `User`.

### Incorrecto

```java
authorize(
    String username,
    String role
);
```

### Correcto

```java
authorize(
    User user,
    ...
);
```

El rol debe obtenerse de:

```text
User.role
```

y no pasarse de forma independiente como un `String`.

---

# No Primitive Identifiers

Los servicios de autorización no deben usar identificadores primitivos como sustituto de relaciones de dominio.

### Incorrecto

```java
authorizeOrderConsultation(
    String userId,
    String orderId
);
```

### Correcto

```java
authorizeOrderConsultation(
    User user,
    Order order
);
```

El mismo principio aplica para:

* Buyer.
* Seller.
* Product.
* Inventory.
* Warehouse.
* Order.
* Shipment.
* Return.
* Refund.

---

# Authorization Scope

La autorización se divide en dos ámbitos que no deben confundirse.

## Read Authorization

Determina si un usuario puede **consultar o leer** información perteneciente a un objeto de dominio.

Ejemplos:

```text
canConsultOrder(User user, Order order)
canConsultProduct(User user, Product product)
canConsultInventory(User user, Inventory inventory)
canConsultWarehouse(User user, Warehouse warehouse)
canConsultAdministrativeReports(User user)
```

La autorización de lectura típicamente depende del rol del usuario y de la relación de propiedad entre el `Buyer`/`Seller` asociado al usuario y el objeto de dominio consultado.

## Execute Authorization

Determina si un usuario puede **ejecutar o modificar** una operación de dominio.

Ejemplos:

```text
canConfirmCart(User user, Cart cart)
canDispatchOrder(User user, Order order)
canRegisterStockMovement(User user, Inventory inventory)
canApproveReturn(User user, Return returnRequest)
canProcessRefund(User user, Refund refund)
```

La autorización de ejecución típicamente depende del rol del usuario, del estado actual del objeto de dominio afectado, y de las reglas de negocio aplicables.

---

# Authorization Responsibilities

Los servicios de autorización son responsables de:

* Determinar si un usuario tiene el rol requerido.
* Validar que el usuario puede **consultar** un objeto de dominio determinado (ámbito de lectura).
* Validar que el usuario puede **ejecutar** una operación de negocio determinada (ámbito de ejecución).
* Validar la autorización según el Domain Model afectado.
* Soportar autorización basada en roles (`UserRole`).
* Soportar reglas de autorización específicas para `Order`, `Product`, `Inventory`, `Warehouse`, `Shipment`, `Return` y `Refund`.
* Soportar autorización relacionada con aprobaciones (`Return`, `Refund`).
* Prevenir que los usuarios ejecuten operaciones fuera de sus responsabilidades (RG-03).
* Prevenir que los usuarios consulten información fuera de su alcance de visibilidad (RG-03).

Los servicios de autorización **no** son responsables de:

* Autenticar credenciales.
* Validar contraseñas.
* Generar tokens JWT.
* Persistir usuarios.
* Ejecutar operaciones de negocio (crear un order, despachar un shipment, etc.).
* Actualizar productos, inventario o pedidos.
* Implementar filtros de seguridad REST.

---

# System Roles

La autorización utiliza el Value Object `UserRole`.

Los roles actualmente definidos son:

```text
BUYER
SELLER
LOGISTICS_OPERATOR
ADMIN
SUPERVISOR
```

El servicio de autorización debe usar el Value Object `UserRole`, nunca strings crudos.

> Recordatorio: `ADMIN` y `SUPERVISOR` no tienen subclase propia en el Domain Model — son instancias de `User` distinguidas únicamente por `role`. Esto no cambia nada en la capa de autorización: la evaluación siempre parte de `User.role`, exista o no una subclase asociada.

---

# Role Authorization Rules

Las siguientes reglas definen lo que cada rol está autorizado a hacer dentro del sistema.

## BUYER

* Puede consultar y operar exclusivamente sobre su propio `Cart`, `Order`, `Return` y `Refund`.
* No puede acceder a información de otros buyers.
* No puede administrar `Inventory`, `Warehouse` ni el catálogo de `Product`.

## SELLER

* Puede consultar y administrar exclusivamente su propio `productCatalog` y sus `associatedWarehouses`.
* Puede consultar el `Inventory` vinculado a sus propias warehouses.
* No puede acceder a productos, warehouses o inventario de otro seller.
* No puede autoregistrarse (RG aplicable a Seller Management); su creación es responsabilidad exclusiva de `ADMIN`.

## LOGISTICS_OPERATOR

* Puede operar exclusivamente sobre la `Warehouse` que tiene asignada (`responsibleUser`) y los `Shipment` originados en ella.
* No puede administrar el catálogo de productos ni el inventario fuera de su warehouse asignada.

## ADMIN

* Puede administrar `Seller` y `Warehouse` en todo el Marketplace, sin restricción de propiedad.
* Es el único rol autorizado para registrar un nuevo `Seller`.

## SUPERVISOR

* Perfil de solo lectura (`consultationScope`): puede consultar información en todo el sistema.
* No está autorizado a ejecutar ninguna operación de modificación (Execute Authorization), únicamente Read Authorization.

---

# User Status

La autorización también debe considerar el `UserStatus` asociado al `User` autenticado.

Los estados soportados son:

```text
ACTIVE
INACTIVE
BLOCKED
```

Un usuario que no esté operativamente activo no debe ser autorizado a ejecutar operaciones de negocio protegidas.

Conceptualmente:

```text
User
 │
 ├── status
 │
 └── role
       │
       ▼
Authorization
```

---

# 1. Authorize Operation

## Description

Determina si un `User` está autorizado para realizar una operación de negocio específica sobre un objeto de dominio determinado.

El contexto de la operación debe estar representado directamente por el Domain Model afectado (`Order`, `Product`, `Inventory`, etc.), no por una entidad genérica de tipo "Operation".

## Input

Conceptualmente:

```text
User
<Domain Model afectado>
```

El servicio no debe recibir:

```java
authorize(
    String userId,
    String targetId,
    String actionType
);
```

## Processing

```text
User
 │
 ├── status
 └── role
       │
       ▼
Authorization Service
       │
       ├── Validate User Status
       │
       ├── Validate Role
       │
       └── Validate Domain Context
```

## Result

El servicio determina si la autorización se concede.

Conceptualmente:

```text
AUTHORIZED
UNAUTHORIZED
```

---

# 2. Authorize Order Operation

## Description

Determina si un `User` puede ejecutar una operación sobre un `Order` (confirmar pago, despachar, finalizar, consultar).

## Input

```text
User
Order
```

## Processing

```text
User
 │
 ▼
Authorization Service
 │
 ├── Validate User Status
 │
 ├── Validate Role (BUYER, LOGISTICS_OPERATOR, ADMIN, SUPERVISOR)
 │
 ├── Validate Buyer ownership (Order.buyer == User asociado) cuando aplica
 │
 └── Validate Order status
```

Un `BUYER` solo puede operar sobre `Order`s cuyo `buyer` corresponda al usuario autenticado. Un `LOGISTICS_OPERATOR` solo puede avanzar el estado de despacho de `Order`s cuyo `Shipment` esté asociado a su `Warehouse` asignada.

---

# 3. Authorize Product Operation

## Description

Determina si un `User` puede ejecutar una operación sobre un `Product` (registrar, actualizar, publicar, suspender, descontinuar).

## Input

```text
User
Product
```

## Processing

```text
User
 │
 ▼
Authorization Service
 │
 ├── Validate User Status
 │
 ├── Validate Role == SELLER (para operaciones de escritura) o SUPERVISOR/ADMIN (solo lectura)
 │
 └── Validate Seller ownership (Product.seller == User asociado)
```

---

# 4. Authorize Inventory Operation

## Description

Determina si un `User` puede ejecutar una operación sobre un `Inventory` (registrar stock inicial, reservar, liberar reserva, registrar movimiento).

## Input

```text
User
Inventory
```

Autorización considera:

* Rol del usuario (`SELLER`, `LOGISTICS_OPERATOR`, `ADMIN`).
* Relación entre `Inventory.warehouse` y la warehouse asociada al `Seller` o al `LogisticsOperator`.

---

# 5. Authorize Warehouse Operation

## Description

Determina si un `User` puede ejecutar una operación sobre una `Warehouse` (registrar, actualizar, asignar operador).

## Input

```text
User
Warehouse
```

Autorización considera:

* `ADMIN` puede operar sobre cualquier `Warehouse`.
* `SELLER` solo puede operar sobre `Warehouse`s donde `owner == Seller` asociado al usuario.
* `LOGISTICS_OPERATOR` solo puede consultar/operar la `Warehouse` donde `responsibleUser` corresponde al usuario.

---

# 6. Authorize Shipment Operation

## Description

Determina si un `User` puede ejecutar una operación sobre un `Shipment` (registrar, actualizar estado, consultar).

## Input

```text
User
Shipment
```

Restringido principalmente a `LOGISTICS_OPERATOR` (cuando `Shipment.operator` corresponde al usuario), `ADMIN` y `SUPERVISOR` (solo lectura).

---

# 7. Authorize Return Operation

## Description

Determina si un `User` puede ejecutar una operación sobre un `Return` (solicitar, aprobar, rechazar, completar).

## Input

```text
User
Return
```

Un `BUYER` solo puede solicitar/consultar `Return`s de sus propios `Order`s. La aprobación, rechazo y finalización requieren rol `ADMIN` o `LOGISTICS_OPERATOR`, según la regla de negocio del proceso posventa.

---

# 8. Authorize Refund Operation

## Description

Determina si un `User` puede ejecutar una operación sobre un `Refund` (procesar, consultar).

## Input

```text
User
Refund
```

Procesar un `Refund` requiere rol `ADMIN`. El `BUYER` propietario del `Return` relacionado solo tiene autorización de lectura sobre su propio `Refund`.

---

# 9. Authorize Seller Operation

## Description

Determina si un `User` puede ejecutar una operación en nombre de o sobre un `Seller` (su propio catálogo, sus warehouses).

## Input

```text
User
Seller
```

La relación relevante está representada en el Domain Model:

```text
User (role = SELLER)
 │
 └── corresponde a → Seller
```

El servicio debe comparar Domain Models, no identificadores primitivos.

---

# 10. Authorize Buyer Operation

## Description

Determina si un `User` puede ejecutar una operación en nombre de o sobre un `Buyer` (su propio carrito, sus pedidos, sus direcciones).

## Input

```text
User
Buyer
```

Aplica el mismo principio que en el servicio anterior: la relación `User (role = BUYER) → Buyer` debe evaluarse mediante Domain Models.

---

# 11. Validate User Authorization Status

## Description

Valida si un `User` está actualmente habilitado para ejecutar operaciones protegidas.

El servicio evalúa:

```text
User.status
```

Los estados soportados son:

```text
ACTIVE
INACTIVE
BLOCKED
```

Solo los usuarios cuyo estado satisface las reglas de autorización pueden proceder. No se requiere consulta a base de datos cuando el estado ya está presente en el Domain Model `User`.

---

# 12. Validate Role Authorization

## Description

Determina si el `UserRole` asociado a un `User` permite la operación solicitada.

El servicio recibe el Domain Model `User` y obtiene:

```text
User.role
```

No debe recibir el rol de forma independiente.

## Conceptual Processing

```text
User
 │
 ▼
User.role
 │
 ▼
UserRole
 │
 ▼
Authorization Rules
 │
 ▼
Authorized / Unauthorized
```

---

# 13. Validate Buyer Ownership

## Description

Determina si un `Buyer` está autorizado a operar sobre un `Cart`, `Order`, `Return` o `Refund` específico.

Conceptualmente:

```text
User
 │
 └── corresponde a → Buyer
                        │
                        ▼
                     Order / Cart / Return / Refund
```

La comparación debe usar relaciones y atributos de dominio (`Order.buyer`, `Cart.buyer`), nunca solo el identificador.

---

# 14. Validate Seller Ownership

## Description

Determina si un `Seller` está autorizado a operar sobre un `Product` o `Warehouse` específico.

Conceptualmente:

```text
User
 │
 └── corresponde a → Seller
                        │
                        ▼
                Product.seller / Warehouse.owner
```

Si se requiere información persistida adicional, el servicio debe usar el Output Port correspondiente.

---

# 15. Validate Logistics Assignment

## Description

Determina si un `LogisticsOperator` está autorizado a operar sobre una `Warehouse` o `Shipment` específico, según su asignación.

Conceptualmente:

```text
User
 │
 └── corresponde a → LogisticsOperator
                        │
                        ▼
        Warehouse.responsibleUser / Shipment.operator
```

---

# Authorization and Business Services

La autorización se ejecuta antes de invocar operaciones de negocio protegidas.

Por ejemplo:

```text
Request
   │
   ▼
Controller
   │
   ▼
Input Port
   │
   ▼
Authorization Service
   │
   ├── Authorized
   │       │
   │       ▼
   │   Business Service (p. ej. Order Management)
   │
   └── Unauthorized
           │
           ▼
       Business Exception
```

El servicio de autorización no ejecuta la operación de negocio.

---

# Authorization and Authentication

La autenticación es responsable de identificar al usuario. La autorización usa el Domain Model `User` ya autenticado.

Conceptualmente:

```text
Credentials
    │
    ▼
Authentication Service (Login)
    │
    ▼
User
    │
    ▼
Authorization Service
    │
    ▼
Business Service
```

El servicio de autorización no debe validar contraseñas. La validación de contraseñas pertenece a los servicios de autenticación (`Login`).

---

# JWT Relationship

El proceso de autenticación produce un JWT tras validar las credenciales exitosamente.

El JWT puede contener información requerida por el mecanismo técnico de seguridad. Sin embargo, los servicios de autorización del dominio deben operar usando el Domain Model `User`.

El dominio no debe depender directamente de:

* Librerías JWT.
* Headers HTTP.
* Filtros de autenticación.
* Frameworks de seguridad.

La conversión de la información de seguridad autenticada a un Domain Model `User` pertenece al límite aplicación/seguridad.

Conceptualmente:

```text
JWT
 │
 ▼
Security Adapter
 │
 ▼
User Domain Model
 │
 ▼
Authorization Service
```

---

# Output Ports

Los servicios de autorización deben usar Output Ports siempre que se requiera información externa.

Posibles Output Ports:

```text
UserRepositoryPort
BuyerRepositoryPort
SellerRepositoryPort
ProductRepositoryPort
InventoryRepositoryPort
WarehouseRepositoryPort
OrderRepositoryPort
ShipmentRepositoryPort
ReturnRepositoryPort
RefundRepositoryPort
```

El repositorio requerido depende de la regla de autorización que se esté evaluando. Por ejemplo:

```text
Autorización de operación sobre Warehouse
        │
        ▼
WarehouseRepositoryPort
```

o:

```text
Autorización de relación Seller-Product
        │
        ▼
ProductRepositoryPort
```

---

# UserRepositoryPort

## Description

Provee información de `User` cuando el Domain Model `User` suministrado no contiene información suficiente para evaluar la autorización.

El puerto canónico es:

```text
UserRepositoryPort
```

definido en `SDD/Domain/Output-ports.md`, con el contrato:

```java
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findByEmail(User user);

    Optional<User> findById(User user);

    boolean existsByEmail(User user);

    void update(User user);
}
```

Los servicios de autorización usan las operaciones de búsqueda orientadas a dominio (`findById`, `findByEmail`), no métodos genéricos de búsqueda por primitivos.

---

# Otros Output Ports

Los siguientes puertos siguen el mismo principio que `UserRepositoryPort` — operan sobre Domain Models, nunca sobre identificadores primitivos, y su contrato completo se define en `SDD/Domain/Output-ports.md`:

```text
BuyerRepositoryPort
SellerRepositoryPort
ProductRepositoryPort
InventoryRepositoryPort
WarehouseRepositoryPort
OrderRepositoryPort
ShipmentRepositoryPort
ReturnRepositoryPort
RefundRepositoryPort
```

Los servicios de autorización nunca acceden directamente a la base de datos; siempre lo hacen a través de estos puertos.

---

# Input Ports

El subdominio de Authorization expone los siguientes casos de uso conceptuales:

```text
AuthorizeOperationUseCase
AuthorizeOrderOperationUseCase
AuthorizeProductOperationUseCase
AuthorizeInventoryOperationUseCase
AuthorizeWarehouseOperationUseCase
AuthorizeShipmentOperationUseCase
AuthorizeReturnOperationUseCase
AuthorizeRefundOperationUseCase
AuthorizeSellerOperationUseCase
AuthorizeBuyerOperationUseCase
ValidateUserAuthorizationStatusUseCase
ValidateRoleAuthorizationUseCase
ValidateBuyerOwnershipUseCase
ValidateSellerOwnershipUseCase
ValidateLogisticsAssignmentUseCase
```

---

# Example Input Ports

```java
interface AuthorizeOrderOperationUseCase {

    void authorize(
        User user,
        Order order
    );
}
```

```java
interface AuthorizeProductOperationUseCase {

    void authorize(
        User user,
        Product product
    );
}
```

```java
interface AuthorizeReturnOperationUseCase {

    void authorize(
        User user,
        Return returnRequest
    );
}
```

```java
interface ValidateSellerOwnershipUseCase {

    void authorize(
        User user,
        Product product
    );
}
```

El tipo de retorno exacto puede definirse según el Domain Model de autorización o la estrategia de excepciones adoptada por el proyecto.

---

# Authorization Flow

## General Flow

```text
Authenticated User
        │
        ▼
      User
        │
        ▼
Authorization Service
        │
        ├── Validate User Status
        │
        ├── Validate UserRole
        │
        ├── Validate Domain Relationships (ownership, assignment)
        │
        ├── Query Output Ports si es necesario
        │
        ▼
Authorization Decision
        │
        ├── Authorized
        │       │
        │       ▼
        │   Business Service
        │
        └── Unauthorized
                │
                ▼
        Authorization Exception
```

---

# Authorization for Order Operations

```text
User
 │
 ▼
Authorization Service
 │
 ├── UserStatus
 ├── UserRole
 ├── Order
 ├── Buyer ownership
 │
 └── OrderRepositoryPort
          │
          ▼
Authorization Decision
```

La operación real sobre el `Order` es ejecutada posteriormente por el servicio de Order Management.

---

# Authorization for Inventory Operations

```text
User
 │
 ▼
Authorization Service
 │
 ├── UserStatus
 ├── UserRole
 ├── Inventory
 ├── Warehouse ownership/assignment
 │
 └── InventoryRepositoryPort
          │
          ▼
Authorization Decision
```

El servicio de Inventory Management sigue siendo responsable de la operación real sobre el inventario.

---

# Authorization Exceptions

Excepciones conceptuales para este subdominio:

```text
UnauthorizedOperationException
UserNotAuthorizedException
UserInactiveException
UserBlockedException
InsufficientRoleException
InvalidAuthorizationContextException
UnauthorizedOrderOperationException
UnauthorizedProductOperationException
UnauthorizedInventoryOperationException
UnauthorizedWarehouseOperationException
UnauthorizedReturnOperationException
UnauthorizedRefundOperationException
```

El catálogo completo de excepciones debe definirse por separado en la documentación de Domain Exceptions.

---

# Separation from Authentication

Las siguientes responsabilidades pertenecen a Authentication:

```text
Validar email
Validar contraseña
Cargar User por email
Generar JWT
```

Las siguientes responsabilidades pertenecen a Authorization:

```text
Validar estado del User
Validar UserRole
Validar permisos de negocio
Validar relaciones de propiedad (Buyer/Seller)
Validar asignación logística
Autorizar operaciones de negocio
Autorizar operaciones de aprobación (Return, Refund)
```

Conceptualmente:

```text
              Authentication
                    │
              email/password
                    │
                    ▼
                  User
                    │
                    ▼
              Authorization
                    │
      ┌─────────┬───┴────┬──────────┬─────────┐
      ▼         ▼        ▼          ▼          ▼
   Order    Product  Inventory  Warehouse   Shipment
```

---

# Validation Strategy

## Standard authorization validation pattern

Todo servicio de autorización sigue el mismo patrón de validación:

```text
Input Domain Models
        |
        v
1. Validate input presence (Domain Models no nulos)
        |
        v
2. Validate User status  (UserStatus, dato de dominio)
        |
        v
3. Validate User role    (UserRole, dato de dominio)
        |
        v
4. Resolve authoritative external state (Output Port, solo cuando es necesario)
        |
        v
5. Validate relationship / ownership / assignment (Domain Models)
        |
        v
6. Decision
        |
        +--> OK   -> return (o void)
        +--> FAIL -> Domain authorization exception
```

La información ya disponible en el dominio (`User.status`, `User.role`) siempre se valida directamente; un Output Port nunca se invoca solo para releer información ya presente en los Domain Models suministrados.

## Validation Matrix

| Authorization Service | User provided | User status (ACTIVE) | Role validation | External info (Output Port) | Relationship validation |
|---|---:|---:|---:|---:|---:|
| ValidateUserAuthorizationStatus | Yes | Yes | No | No | No |
| ValidateRoleAuthorization | Yes | No | Yes (required role) | No | No |
| ValidateBuyerOwnership | Yes | Yes | BUYER | When required | Yes (Order.buyer / Cart.buyer) |
| ValidateSellerOwnership | Yes | Yes | SELLER | When required | Yes (Product.seller / Warehouse.owner) |
| ValidateLogisticsAssignment | Yes | Yes | LOGISTICS_OPERATOR | When required | Yes (Warehouse/Shipment assignment) |
| AuthorizeOperation | Yes | Yes | No | No | No |
| AuthorizeOrderOperation | Yes | Yes | Role-dependent | When required | Yes (buyer/assignment) |
| AuthorizeProductOperation | Yes | Yes | SELLER/ADMIN/SUPERVISOR | No | Yes (ownership) |
| AuthorizeInventoryOperation | Yes | Yes | SELLER/LOGISTICS_OPERATOR/ADMIN | When required | Yes |
| AuthorizeWarehouseOperation | Yes | Yes | Role-dependent | When required | Yes |
| AuthorizeShipmentOperation | Yes | Yes | LOGISTICS_OPERATOR/ADMIN/SUPERVISOR | No | Yes (assignment) |
| AuthorizeReturnOperation | Yes | Yes | BUYER/ADMIN/LOGISTICS_OPERATOR | No | Yes (buyer ownership) |
| AuthorizeRefundOperation | Yes | Yes | ADMIN (execute) / BUYER (read) | No | Yes |
| AuthorizeSellerOperation | Yes | Yes | SELLER/ADMIN | No | Yes |
| AuthorizeBuyerOperation | Yes | Yes | BUYER/ADMIN | No | Yes |

## Service-to-Port Matrix

| Authorization Service | UserRepositoryPort | ProductRepositoryPort | InventoryRepositoryPort | WarehouseRepositoryPort | OrderRepositoryPort | ReturnRepositoryPort |
|---|---:|---:|---:|---:|---:|---:|
| ValidateUserAuthorizationStatus | | | | | | |
| ValidateRoleAuthorization | | | | | | |
| ValidateBuyerOwnership | | | | | ✓ | ✓ |
| ValidateSellerOwnership | | ✓ | | ✓ | | |
| ValidateLogisticsAssignment | | | | ✓ | | |
| AuthorizeOrderOperation | | | | | ✓ | |
| AuthorizeProductOperation | | ✓ | | | | |
| AuthorizeInventoryOperation | | | ✓ | | | |
| AuthorizeWarehouseOperation | | | | ✓ | | |
| AuthorizeReturnOperation | | | | | ✓ | ✓ |

Una celda vacía significa que la decisión de autorización se resuelve exclusivamente con los Domain Models suministrados (datos de dominio), según la regla de la Validation Strategy.

---

# Architectural Constraints

Las siguientes reglas son obligatorias para el subdominio de Authorization:

1. La autorización está separada de la autenticación.
2. La autenticación determina la identidad del usuario.
3. La autorización determina si el usuario autenticado puede realizar una operación.
4. Los servicios de autorización deben recibir Domain Models.
5. Los servicios de autorización nunca deben recibir identificadores primitivos como sustituto de Domain Models.
6. Debe usarse `User` en vez de un `userId` crudo.
7. `UserRole` debe obtenerse de `User.role`.
8. Los roles nunca deben representarse como strings arbitrarios dentro de los servicios de autorización.
9. Cada objeto protegido (`Order`, `Product`, `Inventory`, `Warehouse`, `Shipment`, `Return`, `Refund`) debe usarse como su propio Domain Model — no existe una superclase genérica equivalente a `BankingProduct`.
10. Las relaciones de propiedad (`Seller`↔`Product`/`Warehouse`, `Buyer`↔`Order`/`Cart`/`Return`) deben representarse mediante Domain Models.
11. La asignación logística (`LogisticsOperator`↔`Warehouse`/`Shipment`) debe representarse mediante Domain Models.
12. El estado del usuario debe evaluarse usando `User.status`.
13. Los servicios de autorización deben validar directamente la información de dominio ya disponible.
14. La información externa debe obtenerse a través de Output Ports.
15. Los servicios de autorización nunca deben acceder directamente a bases de datos.
16. Los servicios de autorización nunca deben acceder directamente a JPA, SQL o repositorios de persistencia.
17. Los Output Ports deben pertenecer al dominio.
18. Los adaptadores implementan los Output Ports.
19. Los detalles de implementación de JWT deben permanecer fuera del dominio.
20. Los servicios de autorización no deben validar contraseñas.
21. Los servicios de autorización no deben generar tokens JWT.
22. Los servicios de autorización no deben ejecutar operaciones de negocio.
23. Los servicios de negocio siguen siendo responsables de sus propias reglas de negocio.
24. La autorización debe ocurrir antes de que se ejecuten operaciones de negocio protegidas.
25. La autorización de aprobación de `Return` debe estar separada de la operación real de aprobación.
26. La autorización de procesamiento de `Refund` debe estar separada de la operación real de procesamiento.
27. Los fallos de autorización deben resultar en una excepción de autorización de Domain/Application apropiada.
28. El dominio debe permanecer independiente de frameworks REST y de seguridad.
29. El subdominio de Authorization debe ser completamente testeable sin requerir componentes de infraestructura.