# User and Authentication Management Services

## 1. Introduction

Este documento define los servicios del subdominio **User and Authentication Management** del sistema NexusMarket.

Las principales capacidades de negocio son:

* Login.
* Logout.
* Consult User.
* Update User.
* Change User Status.


Autenticación y autorización son responsabilidades distintas — este documento cubre exclusivamente autenticación (identidad) y gestión genérica del `User`. La autorización (permisos) ya está cubierta en `Authorization Services`.

```text
Authentication (este documento)
     │
     ▼
¿Quién es el User?
     │
     ▼
Authorization (documento aparte)
     │
     ▼
¿Qué puede hacer el User?
```

---

# 2. Domain Model Context

## 2.1 User

```text
User
  |
  +-- identifier
  +-- fullName
  +-- email
  +-- role : UserRole
  +-- status : UserStatus
```

Este subdominio opera sobre `User` de forma genérica — cualquier rol (`BUYER`, `SELLER`, `LOGISTICS_OPERATOR`, `ADMIN`, `SUPERVISOR`). Recordatorio de una decisión ya tomada: `ADMIN` y `SUPERVISOR` no tienen subclase propia, así que las operaciones de este documento aplican por igual a instancias directas de `User` y a instancias de sus subclases (`Buyer`, `Seller`, `LogisticsOperator`).

## 2.2 User.status vs. Buyer.buyerStatus

Es importante no confundir dos atributos que suenan parecido pero son conceptos distintos:

```text
User.status       -> condición de acceso al sistema (ACTIVE, INACTIVE, BLOCKED)
Buyer.buyerStatus -> condición comercial específica del buyer (ENABLED, SUSPENDED)
```

`Change User Status` (este documento) modifica `User.status` — afecta si el usuario puede acceder al sistema en absoluto. `Change Buyer Commercial Status` (Buyer Management Services) modifica `Buyer.buyerStatus` — afecta específicamente su capacidad de comprar, sin bloquear su acceso general. Son operaciones independientes, con reglas y Input Ports distintos; no deben fusionarse.

---

# 3. Password Is Not Part of the Domain Model

`User` (Domain_Model_NexusMarket.md) no define un atributo `password`. Esta es una decisión deliberada: las credenciales de acceso pertenecen al límite de seguridad/infraestructura, no al Domain Model de negocio — mismo principio que en el dominio bancario de referencia ("el dominio no debe depender directamente de JWT, filtros de autenticación, o frameworks de seguridad").

```text
Domain Model (User)          Security Boundary
        |                           |
        |   no contiene password    |
        |                           |  password hash, verificación,
        |                           |  emisión de JWT/sesión
```

Este documento define el servicio de autenticación (`Login`) como una operación que **valida credenciales a través de un adaptador de seguridad** y devuelve un `User` del dominio — no como una operación que compare contraseñas directamente sobre el Domain Model.

---

# 4. Service Design Principle

Cada servicio de este subdominio representa una operación cohesiva, con una separación estricta entre lo que es responsabilidad del dominio (identidad, estado, rol) y lo que es responsabilidad del adaptador de seguridad (contraseñas, tokens, sesiones).

---

# 5. Standard Application Service Pattern

```text
Input credentials / Domain Models
                |
                v
Delegate credential verification to Security Adapter (Login únicamente)
                |
                v
Retrieve authoritative User
                |
                v
Validate User status
                |
                v
Execute Domain behavior (cuando aplica)
                |
                v
Persist through Output Port (cuando aplica)
                |
                v
Return result
```

---

# 6. Input Contract

Incorrecto (para servicios que no son `Login`):

```java
updateUser(String userId, String fullName, String email);
```

Preferido:

```java
updateUser(
    User requestingUser,
    User targetUser
);
```

`Login` es la única excepción legítima a "no recibir primitivos": por su naturaleza, recibe credenciales en texto plano (`email`, `password`) desde el exterior, ya que en ese punto todavía no existe un `User` autenticado que representar como Domain Model.

```java
login(String email, String password); // válido únicamente para este servicio
```

---

# 7. Authoritative State

```text
Input User
       |
       v
UserRepositoryPort
       |
       v
Authoritative User
       |
       v
Business Validation
```

---

# 8. External Information

* `UserRepositoryPort` — recuperar el `User` autoritativo por `email` o `identifier`.
* Security Adapter (fuera del dominio) — verificación de contraseña, generación de JWT/sesión.

---

# 9. User Validation

* `email` único en la plataforma (ya validado en cada subdominio de registro — `Register Buyer`, `Register Seller` — este subdominio no crea usuarios, solo los consulta/actualiza/gestiona).
* `User.status` es válido para la operación solicitada (un usuario `BLOCKED` no puede iniciar sesión).
* Transición de `User.status` válida en `Change User Status`.

---

# 10. Domain Behavior

Preferido:

```java
user.updateContactInformation(fullName, email);
user.activate();
user.deactivate();
user.block();
```

Evitar:

```java
user.setStatus(...);
user.setEmail(...);
```

---

# 11. Input Ports

```text
LoginUseCase
LogoutUseCase
ConsultUserUseCase
UpdateUserUseCase
ChangeUserStatusUseCase
```

---

# 12. Output Ports

```text
UserRepositoryPort
```

El contrato completo de este puerto ya fue definido en `Authorization Services`, sección `UserRepositoryPort` — este subdominio lo reutiliza sin redefinirlo.

## Service-to-Port Matrix

| Service | UserRepositoryPort | Security Adapter (fuera del dominio) |
|---|---:|---:|
| Login | ✓ (findByEmail) | ✓ (verificar password, emitir sesión/JWT) |
| Logout | | ✓ (invalidar sesión/token) |
| Consult User | ✓ (read) | |
| Update User | ✓ (update) | |
| Change User Status | ✓ (update) | |

---

# 13. Login

## 13.1 Purpose

Autentica a un usuario del sistema mediante sus credenciales registradas y establece una sesión autenticada.

## 13.2 Input

```text
String email
String password   // en texto plano, transportado de forma segura hasta el Security Adapter
```

## 13.3 Processing

```text
1. Recibir credenciales.
2. UserRepositoryPort.findByEmail(email) -> User autoritativo.
3. Delegar verificación de password al Security Adapter (nunca comparar en el dominio).
4. Validar User.status == ACTIVE.
5. Security Adapter emite sesión/JWT.
6. Return User (dominio) + token de sesión (infraestructura).
```

## 13.4 Failure Cases

* `email` no encontrado -> rechazar sin revelar si el email existe o no (previene enumeración de usuarios).
* `password` incorrecto -> mismo mensaje genérico que el caso anterior.
* `User.status != ACTIVE` -> rechazar explícitamente (`UserBlockedException`/`UserInactiveException`); este caso sí puede comunicarse de forma distinta, ya que no revela nada sobre la existencia de la cuenta que el usuario no supiera ya (si conoce su propio email).

## 13.5 Domain Boundary

El dominio (`LoginUseCase`) nunca debe:

* Comparar contraseñas directamente.
* Generar o firmar un JWT.
* Conocer detalles de la sesión HTTP.

Esas responsabilidades pertenecen al Security Adapter (ver Authorization Services, sección JWT Relationship, que describe exactamente esta misma separación desde el lado de autorización).

---

# 14. Logout

## 14.1 Purpose

Termina la sesión autenticada de un usuario y evita que se realicen más operaciones a través de esa sesión.

## 14.2 Domain Boundary

Este servicio, en la práctica, tiene una responsabilidad de dominio mínima o nula: invalidar un token/sesión es una operación de infraestructura (p. ej., agregar el JWT a una lista de revocación, o invalidar una sesión de servidor). El Input Port existe para mantener consistencia arquitectónica y trazabilidad, pero su implementación real vive casi enteramente en el Security Adapter.

```text
LogoutUseCase
      |
      v
Security Adapter.invalidateSession(token)
```

---

# 15. Consult User

## 15.1 Purpose

Recupera la información de un usuario de acuerdo con los permisos del usuario solicitante.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative User.
3. Validate User existence.
4. Validate authorization: requestingUser == targetUser (autoconsulta), o Admin/Supervisor.
5. Return User.
```

---

# 16. Update User

## 16.1 Purpose

Actualiza la información mantenida para un usuario existente según las reglas de negocio aplicables.

## 16.2 Required Validations

* `requestingUser == targetUser` (autogestión) o `requestingUser.role == ADMIN`.
* `targetUser` existe.
* Si se actualiza `email`, el nuevo valor es único en la plataforma.

## 16.3 Scope Restriction

Este servicio actualiza únicamente los atributos genéricos de `User` (`fullName`, `email`). No actualiza atributos específicos de subclase (`Buyer.primaryAddress`, `Seller.associatedWarehouses`, etc.) — esos se gestionan en sus propios subdominios (`Update Buyer`, actualización de `Seller` en Seller Management).

## 16.4 Domain Behavior

```java
user.updateContactInformation(fullName, email);
```

---

# 17. Change User Status

## 17.1 Purpose

Cambia el estado operativo de un usuario, como activar, desactivar o bloquear su acceso al sistema.

## 17.2 Required Validations

* `requestingUser.role == ADMIN` — un usuario no puede cambiar su propio `status` (a diferencia de `Update User`, que sí admite autogestión).
* `targetUser` existe.
* Transición de `status` válida.

## 17.3 Default Transitions

```text
ACTIVE   -> INACTIVE
ACTIVE   -> BLOCKED
INACTIVE -> ACTIVE
BLOCKED  -> ACTIVE
```

## 17.4 Domain Behavior

```java
user.activate();
user.deactivate();
user.block();
```

## 17.5 Effect on Other Subdominios

Un `User` con `status == BLOCKED` no debe poder iniciar sesión (§13.4), ni ejecutar ninguna operación protegida en ningún otro subdominio — esta validación ya está cubierta transversalmente por `Authorization Services` (`ValidateUserAuthorizationStatus`), no necesita reimplementarse aquí.

---

# 18. Exception Model

```text
InvalidCredentialsException
UserNotFoundException
DuplicateUserEmailException
UnauthorizedUserOperationException
InvalidUserStatusTransitionException
UserBlockedException
UserInactiveException
```

---

# 19. Persistence Boundary

```text
UserAuthenticationManagementService
        |
        v
UserRepositoryPort
        |
        v
UserPersistenceAdapter
        |
        v
Database
```

`Login` y `Logout` además dependen de un Security Adapter separado (sesión/JWT), fuera de este límite de persistencia de dominio.

---

# 20. Controller Responsibilities

Los controllers no deben implementar:

* Verificación de contraseñas.
* Generación de tokens.
* Reglas de unicidad de email.
* Reglas de transición de `User.status`.
* Persistencia.

---

# 21. Validation Matrix

| Validation | Login | Logout | Consult | Update | Change Status |
|---|---:|---:|---:|---:|---:|
| Credentials verified (Security Adapter) | Yes | N/A | N/A | N/A | N/A |
| User status == ACTIVE | Yes (para permitir login) | N/A | N/A | N/A | N/A |
| Requesting User | N/A | Yes (sesión activa) | Yes | Yes | Yes (Admin only) |
| Authorization (self o Admin) | N/A | N/A | Yes | Yes | Yes (Admin only, sin autogestión) |
| User existence | N/A (se busca por email) | N/A | Yes | Yes | Yes |
| Email uniqueness | N/A | N/A | N/A | When email changes | N/A |
| Status transition validity | N/A | N/A | N/A | N/A | Yes |

---

# 22. Business Rules Summary

## BR-001 — Password Is Never Part of the Domain Model

## BR-002 — Login Delegates Credential Verification to the Security Adapter

## BR-003 — User.status and Buyer.buyerStatus Are Independent Concepts

## BR-004 — Update User Covers Only Generic User Attributes

Atributos específicos de subclase se gestionan en su propio subdominio.

## BR-005 — Self-Management Allowed for Update, Not for Status Change

Un usuario puede actualizar su propia información de contacto, pero no puede cambiar su propio `status`.

## BR-006 — Domain State Changes Through Domain Behavior

## BR-007 — Services Must Be Cohesive

---

# 23. Anti-Patterns

## 23.1 Comparing Passwords in the Domain Layer

Inválido:

```java
if (user.getPassword().equals(inputPassword)) { ... }
```

## 23.2 Generating JWT Inside a Domain Service

## 23.3 Direct Database Access

## 23.4 Self-Service Status Changes

Inválido: permitir que un `User` cambie su propio `status` a través de `Change User Status`.

## 23.5 Mixing User.status and Buyer.buyerStatus

Inválido: que `Change User Status` acepte y module `buyerStatus`, o viceversa.

---

# 24. Testing Requirements

## 24.1 Login Tests

* Credenciales válidas, usuario `ACTIVE` (permitido, sesión emitida).
* Email inexistente (rechazado, mensaje genérico).
* Password incorrecto (rechazado, mismo mensaje genérico que el caso anterior).
* Usuario `BLOCKED`/`INACTIVE` con credenciales correctas (rechazado explícitamente).

## 24.2 Logout Tests

* Sesión válida invalidada exitosamente.
* Intento de logout sin sesión activa (comportamiento definido explícitamente, no ambiguo).

## 24.3 Consult User Tests

* Autoconsulta (permitido).
* Admin consulta cualquier usuario (permitido).
* Usuario intenta consultar a otro usuario sin ser Admin (rechazado).

## 24.4 Update User Tests

* Actualización válida por autogestión.
* Email duplicado al actualizar (rechazado).
* Intento de actualizar atributos de subclase a través de este servicio (fuera de alcance, rechazado o ignorado según el contrato).

## 24.5 Change User Status Tests

* Admin bloquea un usuario `ACTIVE` (permitido).
* Admin reactiva un usuario `BLOCKED`/`INACTIVE` (permitido).
* Usuario intenta cambiar su propio status (rechazado).
* Transición inválida (rechazada).

---

# 25. Definition of Done

* Representa una operación cohesiva.
* Separa estrictamente responsabilidades de dominio (identidad, estado, rol) de responsabilidades de seguridad (contraseñas, tokens, sesiones).
* No expone DTOs REST como contratos de aplicación (excepto `Login`, que recibe credenciales primitivas por necesidad).
* Recupera el `User` autoritativo cuando se requiere.
* Distingue correctamente `User.status` de `Buyer.buyerStatus`.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura de seguridad real (mockeando el Security Adapter).

---

# 26. Final Service Catalog

```text
User and Authentication Management
|
+-- Login
|
+-- Logout
|
+-- Consult User
|
+-- Update User
|
+-- Change User Status
```

---

# 27. Final Design Rule

> **Cada servicio de aplicación de este subdominio es una operación cohesiva responsable de mantener una separación estricta entre identidad y estado de dominio (User) y mecanismos técnicos de seguridad (contraseñas, tokens, sesiones), delegando estos últimos siempre a un Security Adapter fuera del dominio, y persistiendo únicamente a través de Output Ports.**