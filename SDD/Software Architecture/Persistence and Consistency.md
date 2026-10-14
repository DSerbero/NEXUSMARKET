# Persistencia y Consistencia

## 1. Decision

La persistencia inicial de NexusMarket usa MySQL para identidad y operaciones transaccionales, y MongoDB para el catalogo de productos.

| Modelo | Almacenamiento | Regla principal |
|---|---|---|
| `User` | MySQL, tabla `users` | Identidad canonica para todos los roles; `identifier` es la clave primaria y `email` tiene restriccion unica global. |
| `Buyer` | MySQL, tabla `buyers` | Subtipo 1:1 de `User`; comparte el mismo identificador y se guarda en la misma transaccion que la identidad base. |
| Credenciales | MySQL, tabla `user_credentials` | Hash de contrasena asociado a `User`; nunca se persiste la contrasena en texto claro ni como atributo del Domain Model. Los JWT no se almacenan como sesion en esta decision. |
| `Cart` | MySQL, tablas `carts` y `cart_items` | Un unico carrito activo por buyer. Los items mantienen identificadores logicos de productos, sin FK a MongoDB. |
| `Order` | MySQL, tablas `orders`, `order_items` y `order_item_variants` | Orden, buyer e items se guardan transaccionalmente. Cada item conserva una instantanea comercial independiente del catalogo actual. |
| `Product` | MongoDB, coleccion `products` | MongoDB sigue siendo la fuente de verdad del catalogo; las otras bases no crean referencias Mongo como claves foraneas. |

Esta asignacion aprovecha la configuracion existente de MySQL y MongoDB. Mantiene en una sola transaccion relacional los datos que deben cambiar juntos durante el registro de un buyer y la confirmacion de una compra.

## 2. Esquema Relacional

El esquema es conceptual; los nombres de columnas pueden adaptarse a las entidades de persistencia sin cambiar las reglas de propiedad y consistencia.

```text
users
  identifier PK
  full_name
  normalized_email UNIQUE
  role_code
  status_code

buyers
  user_identifier PK, FK -> users.identifier
  primary_address
  buyer_status_code

buyer_additional_addresses
  user_identifier FK -> buyers.user_identifier
  position
  address

user_credentials
  user_identifier PK, FK -> users.identifier
  password_hash

carts
  identifier PK
  buyer_identifier UNIQUE, FK -> buyers.user_identifier
  creation_date
  version

cart_items
  cart_identifier FK -> carts.identifier
  position
  product_identifier   -- referencia logica a products.identifier en MongoDB
  PK (cart_identifier, position)

orders
  identifier PK
  buyer_identifier FK -> buyers.user_identifier
  status_code
  creation_date
  completion_date
  version

order_items
  order_identifier FK -> orders.identifier
  position
  product_identifier  -- identificador historico, sin FK entre bases
  product_type
  unit_price
  seller_identifier
  digital_asset_reference NULL
  PK (order_identifier, position)

order_item_variants
  order_identifier
  item_position
  variant_position
  variant_value
  PK (order_identifier, item_position, variant_position)
  FK (order_identifier, item_position) -> order_items
```

`Buyer` no se guarda como una segunda identidad independiente: `buyers.user_identifier` es simultaneamente su PK y FK a `users`. `UserRepositoryPort` y `BuyerRepositoryPort` deben leer y escribir esta fuente compartida; la restriccion unica de email se aplica en `users`, evitando duplicados entre roles.

Las direcciones adicionales se conservan como filas ordenadas. Las filas de `cart_items` usan `position`, por lo que el almacenamiento preserva el orden y las repeticiones que permite la lista actual del Domain Model. No se asigna semantica de cantidad a esas repeticiones: `Cart` y `Order` no tienen un atributo `quantity`.

## 3. Consistencia Relacional

1. Crear una identidad `User` y su perfil `Buyer` ocurre en una unica transaccion MySQL. El alta falla completa si cualquiera de las dos escrituras falla.
2. Cambiar el carrito actualiza `carts` y `cart_items` en una transaccion MySQL. `version` implementa control optimista; una escritura concurrente con version obsoleta se rechaza y el caso de uso debe recargar antes de reintentar.
3. Confirmar el carrito es una sola operacion de aplicacion (`ConfirmCart`). En la transaccion MySQL debe verificar la version/propietario del carrito, guardar `orders` y sus items, vaciar el carrito y confirmar todo junto. No se permite guardar la orden y vaciar el carrito en commits separados.
4. Las transiciones de estado del pedido actualizan `orders` mediante una transaccion y validan el estado autoritativo leido de MySQL. Una orden finalizada permanece inmutable.
5. Las restricciones PK, FK, UNIQUE y las versiones son la ultima defensa contra duplicados, referencias relacionales rotas y actualizaciones perdidas; las validaciones de los servicios no las reemplazan.

La restriccion `UNIQUE(carts.buyer_identifier)` materializa la decision de un carrito activo por buyer. Las restricciones unicas y FK deben crearse mediante migraciones versionadas; `spring.jpa.hibernate.ddl-auto=validate` valida el esquema y no debe ser sustituido por `update` en produccion.

## 4. Referencias a MongoDB y Snapshot de Orden

`cart_items.product_identifier` es una referencia logica al identificador de `Product` en MongoDB. No se configura una FK SQL ni un DBRef. Para mostrar o confirmar un carrito, el caso de uso obtiene los productos vigentes mediante `ProductRepositoryPort`; si un producto ya no existe o no esta `PUBLISHED`, no se permite confirmar la compra.

Durante la confirmacion, cada producto se vuelve a leer del catalogo y se copian a `order_items` los datos comerciales necesarios para honrar la compra: identificador, tipo, precio unitario, variantes, seller y la referencia de entrega digital cuando aplique. Las consultas posteriores de una orden o factura usan esa instantanea, nunca el precio mutable del documento Mongo. Cambios o eliminaciones posteriores del catalogo no modifican ordenes ya aceptadas.

MySQL y MongoDB no comparten una transaccion ACID. La lectura del catalogo es una validacion externa; la confirmacion de la instantanea en MySQL define el punto de aceptacion de la orden. No se debe afirmar atomicidad global entre las dos bases. Si el negocio exige rechazar cualquier cambio concurrente del catalogo ocurrido entre lectura y commit, antes de implementar esa garantia se requiere un mecanismo versionado de cotizacion/reserva y una coordinacion tipo saga; no una transaccion distribuida implicita.

El inventario fisico no forma parte de estas cuatro tablas. Antes de hacer checkout de productos fisicos, la reserva de inventario debe participar en la misma transaccion MySQL que la orden o adoptar una saga/outbox explicita si su almacenamiento queda fuera de esa base.

## 5. Brechas Antes de Implementar Checkout

La asignacion de almacenamiento queda decidida, pero el codigo aun no ofrece esta consistencia:

- `OrderService.create` recibe un `Cart` del caller y persiste una orden; no vuelve a cargar el carrito autoritativo, no lo vacia ni crea instantaneas de catalogo.
- `CartService` y `OrderService` no estan coordinados por un `ConfirmCart` que comparta una transaccion.
- `Order.items` es `List<Product>` y no declara un tipo de linea/inmutabilidad; el adaptador debe mapearlo a las columnas snapshot, o el modelo de dominio debe incorporar un `OrderItemSnapshot` antes de construir checkout.
- `Product` no tiene moneda ni nombre/descripcion. La moneda de la instantanea y los datos que deben aparecer en una factura/recibo se deben decidir antes de implementar facturacion; no se debe inferir moneda de un `BigDecimal`.
- `Cart` no tiene cantidad. Si el buyer debe indicar unidades, se debe agregar un `quantity` explicito al modelo y al contrato antes de fijar la tabla de lineas.

Estas brechas son requisitos de implementacion, no motivos para duplicar `Product` como agregado relacional ni para introducir transacciones entre MongoDB y MySQL.