# Domain Value Objects

## Introduction

Los Value Objects representan conceptos inmutables dentro del dominio de NexusMarket. A diferencia de las Entities, no tienen identidad propia; se definen completamente por sus atributos.

Estos objetos encapsulan valores de negocio controlados, mejoran la expresividad del dominio y evitan el uso de tipos primitivos o literales de texto dispersos en toda la aplicación.

---

# Value Object Hierarchy

```text
DomainCatalog (Abstract)
├── UserRole
├── UserStatus
├── BuyerStatus
├── ProductType
├── ProductStatus
├── MovementType
├── OrderStatus
├── InvoiceStatus
├── ShipmentStatus
├── ReturnStatus
└── RefundStatus
```

---

# DomainCatalog (Abstract)

## Description

Representa un catálogo de negocio genérico utilizado en todo el dominio de NexusMarket.

Todos los valores de negocio controlados heredan de esta clase, garantizando una estructura consistente en toda la aplicación.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| code | String | Identificador de negocio único. |
| name | String | Nombre legible mostrado dentro de la aplicación. |
| description | String | Definición de negocio del valor del catálogo. |

---

# UserRole

## Description

Representa las responsabilidades y permisos asignados a un usuario del sistema.

Cada usuario tiene exactamente un rol, y los roles determinan qué operaciones de negocio puede realizar (RG-02, RG-03).

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| BUYER | Buyer | Compra productos publicados en el Marketplace. |
| SELLER | Seller | Registra y gestiona productos; incorporado únicamente por un Admin. |
| LOGISTICS_OPERATOR | Logistics Operator | Gestiona la operación física de las warehouses y el despacho. |
| ADMIN | Admin | Gestiona sellers y warehouses. |
| SUPERVISOR | Supervisor | Perfil de solo lectura para consulta y seguimiento. |

---

# UserStatus

## Description

Representa la condición operativa de un usuario dentro del Marketplace.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| ACTIVE | Active | El usuario puede acceder y operar normalmente en el sistema. |
| BLOCKED | Blocked | El acceso del usuario ha sido suspendido. |
| INACTIVE | Inactive | El usuario existe pero actualmente no opera en la plataforma. |

---

# BuyerStatus

## Description

Representa la condición comercial de un buyer para realizar compras.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| ENABLED | Enabled | El buyer puede realizar orders con normalidad. |
| SUSPENDED | Suspended | El buyer tiene restricción temporal para comprar. |

---

# ProductType

## Description

Representa si un producto es físico o digital, determinando si requiere inventario y despacho.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| PHYSICAL | Physical | Requiere control de inventario y despacho. |
| DIGITAL | Digital | Se entrega inmediatamente tras confirmar el pago. |

---

# ProductStatus

## Description

Representa el estado de visibilidad de un producto en el catálogo.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| PUBLISHED | Published | El producto es visible en el catálogo público. |
| SUSPENDED | Suspended | El producto está temporalmente oculto del catálogo. |
| DISCONTINUED | Discontinued | El producto se retira de forma permanente de la venta. |

---

# MovementType

## Description

Representa el tipo de movimiento aplicado a un registro de inventario.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| STOCK_IN | Stock In | Stock ingresado a la warehouse. |
| RESERVATION | Reservation | Stock reservado para un order pendiente. |
| SALE_OUTBOUND | Sale Outbound | Stock retirado por una venta confirmada. |
| ADJUSTMENT | Adjustment | Corrección manual de la cantidad de stock. |
| RETURN_INBOUND | Return Inbound | Stock reingresado tras un return. |

---

# OrderStatus

## Description

Representa la etapa actual de un order dentro de su ciclo de vida. Un order en estado final no puede modificarse bajo ninguna circunstancia.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| CART | Cart | Selección provisional de productos. |
| PENDING_PAYMENT | Pending Payment | En espera de confirmación financiera. |
| PAID | Paid | Pago confirmado; inicia la preparación. |
| DISPATCHED | Dispatched | El order ha salido físicamente de la warehouse. |
| DELIVERED_FINALIZED | Delivered / Finalized | Entrega confirmada; ciclo de vida del order completo. |

---

# InvoiceStatus

## Description

Representa el estado actual de un invoice emitido para un order.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| ISSUED | Issued | El invoice ha sido generado. |
| PAID | Paid | El monto del invoice ha sido recaudado. |
| VOIDED | Voided | El invoice ha sido anulado. |

---

# ShipmentStatus

## Description

Representa el estado actual de un shipment para un order físico.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| IN_PREPARATION | In Preparation | El order está siendo empacado en la warehouse. |
| DISPATCHED | Dispatched | El shipment ha salido de la warehouse. |
| IN_TRANSIT | In Transit | El shipment está en transporte. |
| DELIVERED | Delivered | El shipment llegó al buyer. |

---

# ReturnStatus

## Description

Representa el estado actual de un return solicitado por un buyer.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| REQUESTED | Requested | El return ha sido solicitado. |
| APPROVED | Approved | El return ha sido validado y aceptado. |
| REJECTED | Rejected | La solicitud de return fue rechazada. |
| COMPLETED | Completed | El producto devuelto ha sido reingresado al inventario. |

---

# RefundStatus

## Description

Representa el estado actual de un refund emitido tras un return aprobado.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| PENDING | Pending | El refund ha sido solicitado pero aún no procesado. |
| PROCESSED | Processed | Los fondos han sido devueltos al buyer. |
| REJECTED | Rejected | La solicitud de refund fue rechazada. |

---

# Primitive Enumerations

Los siguientes conceptos son enumeraciones simples porque representan valores técnicos fijos sin comportamiento de negocio.

---

## AddressType

### Description

Representa la clasificación de una dirección de entrega registrada por un buyer.

### Values

- PRIMARY
- ADDITIONAL

---

## NotificationChannel

### Description

Representa el canal de comunicación utilizado por el sistema para notificar eventos de orders y shipments.

### Values

- EMAIL
- SMS
- PUSH_NOTIFICATION

---

# Design Notes

- Todos los catálogos de negocio heredan de **DomainCatalog**.
- Los Value Objects son inmutables.
- La igualdad se determina por sus valores, no por identidad de objeto.
- Las entidades de negocio referencian Value Objects en lugar de cadenas de texto primitivas.
- Las Primitive Enumerations se reservan exclusivamente para conceptos técnicos que no encapsulan reglas o comportamiento de negocio.
- Este enfoque mejora el mantenimiento, la consistencia y la alineación con los principios de Domain-Driven Design (DDD), facilitando la evolución futura del dominio.
