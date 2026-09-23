# Cart Management Services

## 1. Introduction

Este documento define los servicios del subdominio **Cart Management** del sistema NexusMarket.

El subdominio Cart Management es responsable de la selección provisional de productos que un buyer realiza antes de confirmar un pedido formal.

Las principales capacidades de negocio son:

* Add Product to Cart.
* Remove Product from Cart.
* Consult Cart.
* Confirm Cart.

El carrito está representado por el Domain Model `Cart`.

```text
Cart
  |
  +-- buyer : Buyer
  +-- items : List<Product>
  +-- creationDate : LocalDateTime
```

El `Cart` es el primer estado del ciclo de vida de un pedido (Domain Model `Order`, ver Order Management Services):

```text
Cart -> Pending Payment -> Paid -> Dispatched -> Delivered/Finalized
```

---

# 2. Domain Model Context

## 2.1 Cart

```text
Cart
  |
  +-- buyer : Buyer
  +-- items : List<Product>
  +-- creationDate : LocalDateTime
```

## 2.2 One Active Cart per Buyer

La especificación funcional no contempla múltiples carritos simultáneos por buyer. Este documento asume que cada `Buyer` tiene, en todo momento, **un único `Cart` activo**, creado implícitamente en la primera llamada a `Add Product to Cart` si no existe uno ya. Esta es una decisión de diseño que debe confirmarse contra los requisitos reales del proyecto antes de implementarse.

## 2.3 Cart and Order Relationship

`Cart` y `Order` son Domain Models distintos, con su propia lista de `items`. `Confirm Cart` es la operación que traduce el contenido de un `Cart` en un `Order` nuevo — no es una transición de estado del mismo objeto, sino la creación de una entidad diferente a partir de otra (ver §17).

---

# 3. Buyer and Cart Relationship

```text
Requesting User
 |
 | debe corresponder a
 v
Cart.buyer
```

Un `Buyer` solo puede operar sobre su propio `Cart`, sin excepción (RG-03). `ADMIN` y `SUPERVISOR` pueden consultar el `Cart` de cualquier buyer con fines de soporte/reportes, pero nunca modificarlo — modificar el carrito de otra persona no tiene un caso de negocio válido en este dominio, a diferencia de, por ejemplo, un Admin gestionando el catálogo de un seller.

---

# 4. Service Design Principle

Cada servicio de Cart Management representa una operación de negocio cohesiva, responsable de validar todas las condiciones necesarias para ejecutarla correctamente.

---

# 5. Standard Application Service Pattern

```text
Input Domain Models / Value Objects
                |
                v
Retrieve authoritative Cart
                |
                v
Validate requesting User == Cart.buyer
                |
                v
Validate Product (cuando aplica)
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
addProductToCart(
    String buyerId,
    String productId
);
```

Preferido:

```java
addProductToCart(
    Buyer requestingBuyer,
    Product product
);
```

---

# 7. Authoritative State

```text
Input Cart
       |
       v
CartRepositoryPort
       |
       v
Authoritative Cart
       |
       v
Business Validation
```

El servicio nunca debe confiar en el `items` suministrado por el caller para decidir el contenido final del carrito — siempre debe operar sobre el `Cart` recuperado del repositorio.

---

# 8. External Information

Cuando se requiere validar el estado actual de un `Product` (por ejemplo, que siga `PUBLISHED`), el servicio debe usar `ProductRepositoryPort`. Los servicios de aplicación nunca deben acceder directamente a la base de datos.

---

# 9. Cart Validation

* `buyer` existe y está `ACTIVE`.
* `buyer.buyerStatus == ENABLED` — un buyer `SUSPENDED` no puede agregar productos ni confirmar el carrito (ver Buyer Management Services, §17.5).
* `product.status == PUBLISHED` al momento de agregarlo — un producto `SUSPENDED` o `DISCONTINUED` no puede agregarse.
* El carrito no debe quedar en un estado inconsistente (por ejemplo, remover un producto que no está presente debe rechazarse explícitamente, no fallar en silencio).

---

# 10. Domain Behavior

Preferido:

```java
cart.addItem(product);
cart.removeItem(product);
cart.clear();
```

Evitar:

```java
cart.getItems().add(product);
cart.setItems(...);
```

---

# 11. Input Ports

```text
AddProductToCartUseCase
RemoveProductFromCartUseCase
ConsultCartUseCase
ConfirmCartUseCase
```

---

# 12. Output Ports

```text
CartRepositoryPort
ProductRepositoryPort
```

Adicionalmente, `Confirm Cart` coordina con el Input Port `CreateOrderUseCase` del subdominio Order Management (ver §17) — no accede directamente a `OrderRepositoryPort`.

## Service-to-Port Matrix

| Service | CartRepositoryPort | ProductRepositoryPort |
|---|---:|---:|
| Add Product to Cart | ✓ (update) | ✓ (validar status) |
| Remove Product from Cart | ✓ (update) | |
| Consult Cart | ✓ (read) | |
| Confirm Cart | ✓ (read/clear) | ✓ (revalidar status) |

---

# 13. CartRepositoryPort

```java
public interface CartRepositoryPort {

    Cart save(Cart cart);

    Optional<Cart> findByBuyer(Cart cart);

    Cart update(Cart cart);
}
```

---

# 14. Add Product to Cart

## 14.1 Purpose

Agrega un producto al `Cart` de un buyer como selección provisional.

## 14.2 Input

```text
Buyer (requestingBuyer)
Product
```

## 14.3 Validations

* `requestingBuyer.buyerStatus == ENABLED`.
* `product.status == PUBLISHED`.
* Si no existe un `Cart` activo para el buyer, se crea uno nuevo (ver §2.2).

## 14.4 Domain Behavior

```text
Cart
      |
      v
Validate
      |
      v
cart.addItem(product)
      |
      v
CartRepositoryPort.save/update(cart)
```

---

# 15. Remove Product from Cart

## 15.1 Purpose

Elimina un producto previamente seleccionado del `Cart` de un buyer.

## 15.2 Required Validations

* `requestingUser == Cart.buyer`.
* `Cart` existe.
* El producto está efectivamente presente en `Cart.items` (de lo contrario, rechazar explícitamente — no ignorar la solicitud en silencio).

## 15.3 Domain Behavior

```java
cart.removeItem(product);
```

---

# 16. Consult Cart

## 16.1 Purpose

Recupera el contenido actual del `Cart` de un buyer.

## 16.2 Processing

```text
1. Validate requesting User.
2. Retrieve authoritative Cart.
3. Validate authorization: requestingUser == Cart.buyer, o Admin/Supervisor (solo lectura).
4. Return Cart.
```

Si el buyer no tiene un `Cart` activo, el servicio debe devolver un carrito vacío, no lanzar una excepción — la ausencia de carrito no es un error de negocio.

---

# 17. Confirm Cart

## 17.1 Purpose

Convierte el contenido del `Cart` de un buyer en un `Order` formal, iniciando el ciclo de vida del pedido.

## 17.2 Required Validations

* `requestingUser == Cart.buyer`.
* `requestingBuyer.buyerStatus == ENABLED`.
* `Cart.items` no está vacío — no se puede confirmar un carrito sin productos.
* Cada `Product` en `Cart.items` sigue `PUBLISHED` en el momento de la confirmación (revalidación: pudo haberse suspendido o descontinuado desde que se agregó).

## 17.3 Coordination with Order Management

Este servicio no crea el `Order` directamente manipulando `OrderRepositoryPort` — delega esa responsabilidad al Input Port `CreateOrderUseCase` de Order Management, pasándole el `Cart` validado.

```text
ConfirmCartService
        |
        v
Validate Cart / Buyer / Products
        |
        v
CreateOrderUseCase.execute(cart)
        |
        v
Order Management crea el Order (estado inicial: Pending Payment)
        |
        v
Cart.clear()
        |
        v
CartRepositoryPort.update(cart)
```

`Order Management` es quien inicializa la reserva de inventario correspondiente (a través de `ReserveStockUseCase`, ver Inventory Management Services §16.5) — Cart Management no reserva stock directamente.

## 17.4 Post-Confirmation State

Tras una confirmación exitosa, el `Cart` queda vacío (`items = []`), listo para una nueva selección provisional — no se elimina, según la decisión de diseño de §2.2.

---

# 18. Exception Model

```text
CartNotFoundException
EmptyCartException
ProductNotAvailableForCartException
ProductNotInCartException
UnauthorizedCartOperationException
BuyerNotEligibleException
InvalidUserStatusException
```

---

# 19. Persistence Boundary

```text
CartManagementService
        |
        v
CartRepositoryPort
        |
        v
CartPersistenceAdapter
        |
        v
Database
```

---

# 20. Controller Responsibilities

Los controllers no deben implementar:

* Validación de estado del producto.
* Lógica de creación del `Order` a partir del `Cart`.
* Persistencia.

---

# 21. Validation Matrix

| Validation | Add Product | Remove Product | Consult | Confirm |
|---|---:|---:|---:|---:|
| Requesting User == Buyer | Yes | Yes | Yes (o Admin/Supervisor lectura) | Yes |
| Buyer status (ENABLED) | Yes | N/A | N/A | Yes |
| Product status (PUBLISHED) | Yes | N/A | N/A | Yes (revalidado) |
| Cart existence | N/A (se crea) | Yes | N/A (vacío si no existe) | Yes |
| Cart not empty | N/A | N/A | N/A | Yes |

---

# 22. Business Rules Summary

## BR-001 — Cart Belongs Exclusively to Its Buyer

Ningún otro `Buyer` puede operar sobre un `Cart` ajeno.

## BR-002 — Only PUBLISHED Products Can Be Added

## BR-003 — Product Status Is Revalidated at Confirmation Time

Un producto pudo cambiar de estado entre que se agregó al carrito y que se confirma.

## BR-004 — Confirm Cart Delegates Order Creation

`Cart Management` no crea `Order`s directamente; coordina con `Order Management` a través de su Input Port.

## BR-005 — Cart Is Cleared, Not Deleted, After Confirmation

## BR-006 — Domain State Changes Through Domain Behavior

## BR-007 — Services Must Be Cohesive

---

# 23. Anti-Patterns

## 23.1 Direct Database Access

## 23.2 Primitive Application Contracts

## 23.3 Trusting Cart Contents Without Revalidating Products

Inválido: confirmar un `Cart` sin volver a validar que cada producto sigue `PUBLISHED`.

## 23.4 Confirm Cart Creating the Order Directly

Inválido:

```text
ConfirmCartService
     |
     +-- new Order(...)
     +-- OrderRepositoryPort.save(order)
```

Debe delegarse a `CreateOrderUseCase`.

## 23.5 Silent No-Op on Invalid Removal

Inválido: que `Remove Product from Cart` retorne éxito sin error cuando el producto no estaba en el carrito.

---

# 24. Testing Requirements

## 24.1 Add Product to Cart Tests

* Producto `PUBLISHED` agregado exitosamente.
* Producto `SUSPENDED`/`DISCONTINUED` (rechazado).
* Buyer `SUSPENDED` intenta agregar (rechazado).
* Primer producto agregado crea el `Cart` si no existía.

## 24.2 Remove Product from Cart Tests

* Remoción válida.
* Intento de remover un producto no presente (rechazado).
* Usuario distinto al buyer intenta remover (rechazado).

## 24.3 Consult Cart Tests

* Buyer consulta su propio carrito.
* Carrito vacío devuelto cuando no existe (sin error).
* Otro buyer intenta consultar (rechazado).

## 24.4 Confirm Cart Tests

* Confirmación válida con productos disponibles.
* Carrito vacío (rechazado).
* Producto cambió a `SUSPENDED` entre agregarlo y confirmar (rechazado).
* Buyer `SUSPENDED` intenta confirmar (rechazado).
* Tras confirmación exitosa, el carrito queda vacío.

---

# 25. Definition of Done

* Representa una operación de negocio coherente.
* Recibe los Domain Models apropiados.
* No expone DTOs REST como contratos de aplicación.
* Recupera el `Cart` autoritativo antes de mutarlo.
* Valida propiedad del carrito y estado del buyer/producto.
* Delega la creación del `Order` al subdominio correspondiente.
* Ejecuta comportamiento de dominio válido.
* Persiste a través de Output Ports.
* Puede probarse sin infraestructura.

---

# 26. Final Service Catalog

```text
Cart Management
|
+-- Add Product to Cart
|
+-- Remove Product from Cart
|
+-- Consult Cart
|
+-- Confirm Cart
```

---

# 27. Final Design Rule

> **Cada servicio de aplicación es una operación de negocio cohesiva responsable de determinar si la operación puede ejecutarse, validando la propiedad del Cart, el estado del Buyer y la disponibilidad vigente de cada Product, ejecutando comportamiento de dominio válido, delegando la creación del Order al subdominio correspondiente, y persistiendo a través de Output Ports.**