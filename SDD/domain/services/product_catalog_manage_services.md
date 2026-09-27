# Product Catalog Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Product Catalog Management** del sistema NexusMarket.

El subdominio Product Catalog Management es responsable de gestionar el catálogo de productos ofrecidos por los sellers.

Las principales capacidades de negocio son:

* Register Product.
* Consult Product.
* Update Product.
* Publish Product.
* Suspend Product.
* Discontinue Product.

Los productos están representados por el Domain Model `Product` (abstracto), especializado en `PhysicalProduct` y `DigitalProduct`.

```text
Product (Abstract)
  |
  +-- PhysicalProduct
  +-- DigitalProduct
```

A diferencia de `Warehouse`, esta división sí está justificada: `PhysicalProduct` requiere inventario y despacho (`associatedInventory`), mientras que `DigitalProduct` se entrega de forma inmediata (`digitalAsset`) sin pasar por una warehouse — son atributos y comportamientos genuinamente distintos, no solo un valor diferente.

Un `Product` pertenece siempre a un `Seller`:

```text
Product.seller : Seller
```

---

# 2. Domain Model Context

## 2.1 Product (Abstract)

```text
Product (Abstract)
  |
  +-- variants : List<String>
  +-- status : ProductStatus
  +-- seller : Seller
```

## 2.2 PhysicalProduct

```text
PhysicalProduct
  |
  +-- associatedInventory : Inventory
```

## 2.3 DigitalProduct

```text
DigitalProduct
  |
  +-- digitalAsset : String
```

## 2.4 Product Status Lifecycle

```text
PUBLISHED
   |
   v
SUSPENDED  <---->  PUBLISHED
   |
   v
DISCONTINUED
```

`DISCONTINUED` es un estado terminal: un producto descontinuado no puede volver a `PUBLISHED` ni a `SUSPENDED`.

---

# 3. Seller and Product Relationship

```text
Requesting User
 |
 | debe corresponder a
 v
Product.seller
```

Solo el `Seller` dueño del producto puede ejecutar operaciones de escritura sobre él (`Register`, `Update`, `Publish`, `Suspend`, `Discontinue`). `ADMIN` y `SUPERVISOR` tienen acceso de lectura sin restricción de propiedad; `ADMIN` no gestiona el catálogo de un seller en su nombre bajo el flujo normal — esa es una decisión de negocio que, de requerirse (por ejemplo, para retirar un producto por incumplimiento), debe tratarse como una regla explícita y no asumirse por defecto.

---

# 4. Service Design Principle

Cada servicio de Product Catalog Management representa una operación de negocio cohesiva, responsable de validar todas las condiciones necesarias para ejecutarla correctamente.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative state
                |
                v
Validate requesting User (Seller dueño, o Admin/Supervisor para lectura)
                |
                v
Validate Product data
                |
                v
Validate status transition (cuando aplica)
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
registerProduct(
    String sellerId,
    String type,
    List<String> variants
);
```

Preferido:

```java
registerProduct(
    User requestingUser,
    Product newProduct   // instancia concreta: PhysicalProduct o DigitalProduct
);
```

El tipo concreto (`PhysicalProduct` vs. `DigitalProduct`) se determina por la clase del Domain Model recibido — no por un campo `productType` adicional, ya que esa información ya la expresa el tipo (decisión de diseño ya tomada sobre `Product`).

---

# 7. Authoritative State

```text
Input Product
       |
       v
ProductRepositoryPort
       |
       v
Authoritative Product
       |
       v
Business Validation
```

Particularmente importante para `status` — el servicio nunca debe confiar en el estado suministrado por el caller para decidir si una transición es válida.

---

# 8. External Information

Cuando se requiere validar que el `seller` suministrado corresponde a un `Seller` real y activo, el servicio debe usar `SellerRepositoryPort`. Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Product Validation

* `seller` existe y está `ACTIVE`.
* `price` es mayor que cero.
* `variants` es una lista válida (puede estar vacía si el producto no tiene variantes).
* Para `PhysicalProduct`: no se exige `associatedInventory` en el momento del registro — el inventario se crea por separado en el subdominio Inventory Management (Dominio 6: "el inventario debe estar vinculado obligatoriamente a un producto y una bodega específica", como paso posterior al registro del producto).
* Para `DigitalProduct`: `digitalAsset` no está vacío.
* `status` inicial siempre es `PUBLISHED`, salvo que la regla de negocio del proyecto requiera revisión previa; si es así, debe ser una decisión explícita, no un valor por defecto implícito.

---

# 10. Domain Behavior

Preferido:

```java
product.updateVariants(newVariants);
product.publish();
product.suspend();
product.discontinue();
```

Evitar:

```java
product.setStatus(...);
product.setVariants(...);
```

El Domain Model debe proteger la transición de estados — por ejemplo, `product.publish()` debe lanzar una excepción de dominio si el producto ya está `DISCONTINUED`.

---

# 11. Input Ports

```text
RegisterProductUseCase
ConsultProductUseCase
UpdateProductUseCase
PublishProductUseCase
SuspendProductUseCase
DiscontinueProductUseCase
```

---

# 12. Output Ports

```text
ProductRepositoryPort
SellerRepositoryPort
```

## Service-to-Port Matrix

| Service | ProductRepositoryPort | SellerRepositoryPort |
|---|---:|---:|
| Register Product | ✓ (save) | ✓ (validar seller) |
| Consult Product | ✓ (read) | |
| Update Product | ✓ (update) | |
| Publish Product | ✓ (update) | |
| Suspend Product | ✓ (update) | |
| Discontinue Product | ✓ (update) | |

---

# 13. ProductRepositoryPort

```java
public interface ProductRepositoryPort {

    Product save(Product product);

    Optional<Product> findByIdentifier(Product product);

    List<Product> findBySeller(Product product);

    Product update(Product product);
}
```

`findBySeller` es usado por `Consult Seller Catalog` (Seller Management) y por validaciones de propiedad en Authorization.

---

# 14. Register Product

## 14.1 Purpose

Crea un nuevo producto (físico o digital) y lo asocia al catálogo de un seller.

## 14.2 Input

```text
User (requestingUser)
Product (newProduct — PhysicalProduct o DigitalProduct)
```

## 14.3 Validations

* `requestingUser.role == SELLER`.
* `newProduct.seller == requestingUser` (un seller no puede registrar un producto en nombre de otro).
* Validaciones específicas del tipo concreto (ver §9).

## 14.4 Domain Creation

```text
Product
      |
      v
Validate
      |
      v
Create Domain State (status = PUBLISHED, seller = requestingUser)
      |
      v
ProductRepositoryPort.save(product)
```

---

# 15. Consult Product

## 15.1 Purpose

Recupera un `Product` de acuerdo con los permisos del usuario solicitante.

## 15.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Product.
3. Validate Product existence.
4. Validate authorization: cualquier usuario autenticado puede consultar un producto PUBLISHED
   (es un catálogo público); SUSPENDED/DISCONTINUED solo son visibles para el Seller dueño, Admin y Supervisor.
5. Return Product.
```

Esta es una diferencia importante frente a los demás subdominios: `Consult Product` para productos `PUBLISHED` **no** está restringido por propiedad — el catálogo público, por definición, es visible para cualquier `Buyer`. La restricción de propiedad solo aplica a estados no públicos.

---

# 16. Update Product

## 16.1 Purpose

Actualiza la información mantenida para un producto existente, como sus variantes.

## 16.2 Required Validations

* `requestingUser == Product.seller`.
* `Product` existe.
* `Product.status != DISCONTINUED` (un producto descontinuado no puede editarse).

## 16.3 Scope Restriction

Este servicio no permite cambiar el tipo del producto (de `PhysicalProduct` a `DigitalProduct` o viceversa) — eso no es una actualización, sería crear un producto distinto. Tampoco permite cambiar `seller` (transferencia de producto entre sellers está fuera de alcance).

## 16.4 Domain Behavior

```java
product.updateVariants(newVariants);
```

---

# 17. Publish Product

## 17.1 Purpose

Cambia el estado de un producto a `PUBLISHED`, haciéndolo visible en el catálogo público.

## 17.2 Required Validations

* `requestingUser == Product.seller`.
* `Product` existe.
* Transición válida: `SUSPENDED -> PUBLISHED`. `PUBLISHED -> PUBLISHED` y `DISCONTINUED -> PUBLISHED` deben rechazarse.

## 17.3 Domain Behavior

```java
product.publish();
```

---

# 18. Suspend Product

## 18.1 Purpose

Cambia el estado de un producto a `SUSPENDED`, ocultándolo temporalmente del catálogo público.

## 18.2 Required Validations

* `requestingUser == Product.seller`.
* `Product` existe.
* Transición válida: `PUBLISHED -> SUSPENDED`. `DISCONTINUED -> SUSPENDED` debe rechazarse.

## 18.3 Domain Behavior

```java
product.suspend();
```

---

# 19. Discontinue Product

## 19.1 Purpose

Retira de forma permanente un producto de la venta.

## 19.2 Required Validations

* `requestingUser == Product.seller`.
* `Product` existe.
* Transición válida: `PUBLISHED -> DISCONTINUED` o `SUSPENDED -> DISCONTINUED`. Ya `DISCONTINUED` es un estado terminal — repetir la operación debe rechazarse.

## 19.3 Domain Behavior

```java
product.discontinue();
```

## 19.4 Effect on Other Subdominios

Un producto `DISCONTINUED` no debe poder agregarse a un `Cart` nuevo (`Cart Management`) ni permitir nuevas reservas de inventario (`Inventory Management`) — esa validación pertenece a esos subdominios, que deben consultar `Product.status` como parte de su propia lógica de negocio.

---

# 20. Exception Model

```text
ProductNotFoundException
InvalidProductDataException
InvalidProductPriceException
UnauthorizedProductOperationException
InvalidProductStatusTransitionException
ProductAlreadyDiscontinuedException
InvalidUserStatusException
```

---

# 21. Persistence Boundary

```text
ProductCatalogManagementService
        |
        v
ProductRepositoryPort
        |
        v
ProductPersistenceAdapter
        |
        v
Database
```

---

# 22. Controller Responsibilities

Los controllers no deben implementar:

* Validación de propiedad del seller.
* Reglas de transición de estado.
* Persistencia.

---

# 23. Validation Matrix

| Validation | Register | Consult | Update | Publish | Suspend | Discontinue |
|---|---:|---:|---:|---:|---:|---:|
| Requesting User | Yes | Yes | Yes | Yes | Yes | Yes |
| User status | Yes | Yes | Yes | Yes | Yes | Yes |
| Authorization (ownership) | Yes | Solo para no-PUBLISHED | Yes | Yes | Yes | Yes |
| Product existence | N/A | Yes | Yes | Yes | Yes | Yes |
| Status transition validity | N/A (estado inicial) | N/A | N/A (bloqueado si DISCONTINUED) | Yes | Yes | Yes |

---

# 24. Business Rules Summary

## BR-001 — Product Is Abstract, PhysicalProduct/DigitalProduct Are Justified Subtypes

A diferencia de `Warehouse`, esta jerarquía se mantiene porque los subtipos tienen atributos y comportamiento genuinamente distintos.

## BR-002 — Product Type Is Determined by Domain Model Type

No existe un atributo `productType` redundante; el tipo concreto del objeto ya expresa esa información.

## BR-003 — Only the Owning Seller Can Modify a Product

## BR-004 — Discontinued Is a Terminal State

## BR-005 — Published Products Are Publicly Visible

`Consult Product` no restringe por propiedad cuando `status == PUBLISHED`.

## BR-006 — Domain State Changes Through Domain Behavior

## BR-007 — External Information Uses Output Ports

## BR-008 — Services Must Be Cohesive

---

# 25. Anti-Patterns

## 25.1 Direct Database Access

## 25.2 Primitive Application Contracts

## 25.3 Trusting Caller-Supplied Status

No asumir que `Product.status` suministrado por el caller es el estado persistido actual antes de validar una transición.

## 25.4 Reintroducing productType as a Field

Inválido: agregar de nuevo un atributo `productType` a `Product` — ya se decidió que esa información la da el tipo concreto (`PhysicalProduct`/`DigitalProduct`).

## 25.5 Allowing Invalid Status Transitions

Inválido: permitir `DISCONTINUED -> PUBLISHED` sin una regla de negocio explícita que lo autorice.

---

# 26. Testing Requirements

## 26.1 Register Product Tests

* Registro válido de `PhysicalProduct`.
* Registro válido de `DigitalProduct`.
* Seller intenta registrar producto para otro seller (rechazado).
* `DigitalProduct` con `digitalAsset` vacío (rechazado).

## 26.2 Consult Product Tests

* Cualquier usuario consulta un producto `PUBLISHED` (permitido).
* Buyer intenta consultar un producto `SUSPENDED` de otro seller (rechazado).
* Seller dueño consulta su propio producto `SUSPENDED` (permitido).

## 26.3 Update Product Tests

* Actualización válida de variantes.
* Intento de actualizar producto `DISCONTINUED` (rechazado).
* Seller no-dueño intenta actualizar (rechazado).

## 26.4 Publish / Suspend / Discontinue Tests

* `SUSPENDED -> PUBLISHED` (permitido).
* `PUBLISHED -> PUBLISHED` (rechazado).
* `PUBLISHED -> SUSPENDED` (permitido).
* `PUBLISHED -> DISCONTINUED` (permitido).
* `SUSPENDED -> DISCONTINUED` (permitido).
* `DISCONTINUED -> PUBLISHED` (rechazado).
* `DISCONTINUED -> SUSPENDED` (rechazado).
* `DISCONTINUED -> DISCONTINUED` (rechazado).

---

# 27. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models apropiados (`PhysicalProduct`/`DigitalProduct` concretos, no un `Product` genérico con un campo de tipo).
* No expone DTOs REST como contratos de aplicación.
* Recupera estado autoritativo cuando se requiere.
* Valida al usuario solicitante y la propiedad del producto.
* Valida las transiciones de estado según el ciclo de vida definido.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 28. Final Service Catalog

```text
Product Catalog Management
|
+-- Register Product
|
+-- Consult Product
|
+-- Update Product
|
+-- Publish Product
|
+-- Suspend Product
|
+-- Discontinue Product
```

---

# 29. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando todas las condiciones requeridas de usuario, propiedad del Seller, y validez de la transición de estado del Product, ejecutando comportamiento de dominio válido, y persistiendo a través de Output Ports.**