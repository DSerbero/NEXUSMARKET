# Domain Model

## Introduction

El Domain Model representa las entidades de negocio centrales del sistema Marketplace NexusMarket. Estas entidades encapsulan las reglas de negocio, los datos y las relaciones descritas en la especificación funcional.

El modelo sigue principios de Diseño Orientado a Objetos y aplica herencia para eliminar información duplicada, promoviendo la reutilización y el mantenimiento.

---

# Domain Class Hierarchy

```text
User
├── Buyer
├── Seller
└── LogisticsOperator

Product (Abstract)
├── PhysicalProduct
└── DigitalProduct

Warehouse

Inventory
Cart
Order
Invoice
Shipment
Return
Refund
```

---

# Entities

---

# User

## Description

Representa a cualquier persona autorizada para interactuar con el sistema NexusMarket. Centraliza la información de identificación y acceso común a todos los roles de la plataforma.

Cada usuario tiene exactamente un rol y solo puede gestionar información dentro del alcance de ese rol (reglas de negocio RG-02, RG-03).

Los roles `ADMIN` y `SUPERVISOR` no tienen atributos adicionales más allá de los definidos aquí, por lo que se representan como instancias directas de `User` (identificadas por su `role`), sin una subclase propia. `Buyer`, `Seller` y `LogisticsOperator` sí requieren subclase porque agregan atributos y relaciones que no aplican al resto de los usuarios.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Identifica de forma única al usuario en toda la plataforma. |
| fullName | String | Nombre oficial del usuario. |
| email | String | Canal principal de acceso y comunicación. Debe ser único. |
| role | UserRole | Define las responsabilidades y permisos del usuario. Único por usuario. |
| status | UserStatus | Condición operativa del usuario (Activo, Bloqueado, etc.). |

---

# Buyer

## Description

Representa a un cliente que compra productos publicados en el Marketplace.

Un buyer nunca gestiona información de otros buyers ni datos de inventario.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| primaryAddress | String | Ubicación habitual para las entregas. |
| additionalAddresses | List\<String\> | Ubicaciones de entrega secundarias. |
| buyerStatus | BuyerStatus | Condición del buyer para realizar compras. |

---

# Seller

## Description

Representa a la parte responsable de registrar y gestionar productos en el Marketplace.

Los sellers no pueden autoregistrarse; son incorporados exclusivamente por un Admin.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| associatedWarehouses | List\<Warehouse\> | Bodegas vinculadas a la operación del seller. |
| productCatalog | List\<Product\> | Productos registrados y gestionados por el seller. |

---

# LogisticsOperator

## Description

Representa a la parte encargada de la operación física de las bodegas y del despacho de pedidos.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| assignedWarehouse | Warehouse | Bodega bajo responsabilidad del operator. |

---

# Product (Abstract)

## Description

Representa cualquier bien ofrecido para la venta en el Marketplace, ya sea físico o digital.

Esta clase no puede instanciarse directamente.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| productType | ProductType | Físico o Digital. |
| variants | List\<String\> | Diferencias como color, talla, modelo, etc. |
| status | ProductStatus | Publicado, Suspendido o Descontinuado. |
| seller | Seller | Seller propietario y responsable del producto. |

---

# PhysicalProduct

## Description

Representa un producto tangible que requiere control de inventario y despacho físico a través de una warehouse.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| associatedInventory | Inventory | Registro de stock que vincula el producto con una warehouse. |

---

# DigitalProduct

## Description

Representa un producto intangible que se entrega inmediatamente tras la confirmación del pago, sin pasar por una warehouse.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| digitalAsset | String | Referencia al recurso digital entregado al buyer. |

---

# Warehouse

## Description

Representa una ubicación física donde se almacena y gestiona el inventario.

Una warehouse puede pertenecer directamente al Marketplace o a un seller; esta distinción no cambia su comportamiento ni sus reglas de negocio, por lo que se modela como un atributo (`ownerType`) en lugar de como subclases separadas.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | long | Identifica de forma única a la warehouse. |
| location | String | Ubicación física de la warehouse. |
| ownerType | WarehouseOwnerType | Indica si la warehouse pertenece al Marketplace o a un seller. |
| responsibleUser | User | Usuario responsable de la operación diaria de la warehouse. |

---

# Inventory

## Description

Representa el stock distribuido de un producto. Debe estar siempre vinculado tanto a un Product como a una Warehouse, y en ningún caso se permite stock negativo.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| product | PhysicalProduct | Producto al que pertenece este registro de inventario. |
| warehouse | Warehouse | Bodega donde se almacena físicamente el stock. |
| availableQuantity | Integer | Unidades actualmente disponibles para comercialización. |
| movementType | MovementType | Stock In, Reservation, Sale Outbound, Adjustment o Return Inbound. |
| movementDate | LocalDateTime | Fecha y hora del último movimiento de inventario. |

---

# Cart

## Description

Representa la selección provisional de productos realizada por un buyer antes de confirmar un order. Es el estado inicial del ciclo de vida del order.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| buyer | Buyer | Propietario del cart. |
| items | List\<Product\> | Productos seleccionados provisionalmente. |

---

# Order

## Description

Representa el compromiso comercial formal entre un buyer y el Marketplace. Es la entidad central del sistema y avanza a través de un ciclo de vida definido: Cart → Pending Payment → Paid → Dispatched → Delivered/Finalized. Un order finalizado no puede modificarse bajo ninguna circunstancia.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| buyer | Buyer | Buyer que realizó el order. |
| items | List\<Product\> | Productos incluidos en el order. |
| orderStatus | OrderStatus | Etapa actual del ciclo de vida del order. |
| creationDate | LocalDateTime | Fecha y hora de creación del order. |
| completionDate | LocalDateTime | Fecha y hora de finalización del order. |

---

# Invoice

## Description

Representa la información comercial y financiera asociada a una venta ya confirmada.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| order | Order | Order por el cual se emite este invoice. |
| totalAmount | BigDecimal | Monto total facturado. |
| issueDate | LocalDateTime | Fecha y hora de emisión del invoice. |
| invoiceStatus | InvoiceStatus | Estado actual del invoice. |

---

# Shipment

## Description

Representa el proceso logístico —empaque, despacho y transporte— aplicado a los orders que contienen productos físicos.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| order | Order | Order que está siendo enviado. |
| originWarehouse | Warehouse | Bodega desde la cual parte el envío. |
| dispatchManager | User | Persona responsable del despacho. |
| shipmentStatus | ShipmentStatus | Estado actual del envío. |
| dispatchDate | LocalDateTime | Fecha y hora en que el envío salió de la warehouse. |

---

# Return

## Description

Representa el proceso posventa iniciado cuando un buyer devuelve un producto ya entregado. Está sujeto a validaciones críticas sobre el inventario relacionado.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| order | Order | Order asociado al return. |
| reason | String | Motivo de la devolución. |
| returnStatus | ReturnStatus | Estado actual del return. |
| requestDate | LocalDateTime | Fecha y hora de solicitud del return. |

---

# Refund

## Description

Representa el reembolso financiero emitido a un buyer como resultado de un return aprobado.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| relatedReturn | Return | Return que dio origen al refund. |
| refundedAmount | BigDecimal | Monto devuelto al buyer. |
| refundStatus | RefundStatus | Estado actual del refund. |
| processingDate | LocalDateTime | Fecha y hora en que se procesó el refund. |
