# Domain Model

## Introduction

he Domain Model represents the core business entities of the NexusMarket Marketplace System. These entities encapsulate the business rules, data, and relationships described in the functional business specification.

The model follows Object-Oriented Design principles and applies inheritance to eliminate duplicated information while promoting reusability and maintainability.

---

# Domain Class Hierarchy
User (Abstract)
├── Seller
├── Buyer
├── LogisticsOp
├── Administrator
└── Supervisor

Warehouse (Abstract)
├── MarketplaceWa
└── SellersWa

Product (Abstract)
├── FisicPro
└── DigitalPro


Inventory

ShoppingCart

Order

Invoice

Shipping

Returns

Refunds
---

# Entities

---

# User (Abstract)
 
## Description
 
Represents any person authorized to interact with the NexusMarket system. This abstract class centralizes all common identification and access information shared by every role in the platform (Comprador, Vendedor, Operador, Admin, Supervisor).
 
Each user has exactly one role and can only manage information within the scope of that role (business rules RG-02, RG-03).
 
This class cannot be instantiated directly.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| identificador | String | Uniquely identifies the user across the platform. |
| nombreCompleto | String | Official name of the user. |
| correoElectronico | String | Primary access and communication channel. Must be unique. |
| rol | UserRole | Defines the responsibilities and permissions of the user. Unique per user. |
| estado | UserStatus | Operational condition of the user (Active, Blocked, etc.). |
 
---
 
# Buyer
 
## Description
 
Represents a customer who purchases products published on the Marketplace.
 
A comprador never manages information belonging to other compradores nor inventory data.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| direccionPrincipal | String | Usual location for deliveries. |
| direccionesAdicionales | List\<String\> | Secondary delivery locations. |
| estadoComercial | BuyerStatus | Condition of the comprador for making purchases. |
 
---
 
# Seller
 
## Description
 
Represents a party responsible for registering and managing products on the Marketplace.
 
Vendedores cannot self-register; they are onboarded exclusively by an Admin.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| bodegasAsociadas | List\<Bodega\> | Warehouses linked to the vendedor's operation. |
| catalogoProductos | List\<Producto\> | Products registered and managed by the vendedor. |
 
---
 
# LogisticsOp
 
## Description
 
Represents the party in charge of the physical operation of bodegas and order dispatch (Operador Logístico).
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| bodegaAsignada | Bodega | Warehouse the operador is responsible for. |
 
---
 
# Administrator
 
## Description
 
Represents the administrator responsible for managing vendedores and bodegas across the Marketplace.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| permisosAdministrativos | List\<String\> | Administrative capabilities granted to the account. |
 
---
 
# Supervisor
 
## Description
 
Represents a read-only profile used for consultation and operational monitoring across the system.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| ambitoConsulta | String | Scope of information the supervisor is authorized to view. |
 
---
 
# Product (Abstract)
 
## Description
 
Represents any good offered for sale on the Marketplace, either physical or digital.
 
This class cannot be instantiated directly.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| tipoProducto | ProductType | Physical or Digital. |
| variantes | List\<String\> | Differences such as color, size, model, etc. |
| estado | ProductStatus | Published, Suspended, or Discontinued. |
| vendedor | Vendedor | Vendedor who owns and manages the product. |
 
---
 
# FisicPro
 
## Description
 
Represents a tangible product that requires inventory tracking and physical dispatch through a bodega.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| inventarioAsociado | Inventario | Stock record linking the product to a bodega. |
| requiereEnvio | Boolean | Indicates the product must go through the logistics flow. |
 
---
 
# DigitalPro
 
## Description
 
Represents an intangible product delivered immediately upon payment confirmation, without involving a bodega.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| entregaInmediata | Boolean | Confirms the product is delivered right after payment. |
| recursoDigital | String | Reference to the digital asset delivered to the comprador. |
 
---
 
# Warehouse (Abstract)
 
## Description
 
Represents a physical location where inventory is stored and managed.
 
Bodegas are classified according to who administers them: the Marketplace itself or a vendedor.
 
This class cannot be instantiated directly.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| identificador | String | Uniquely identifies the bodega. |
| ubicacion | String | Physical location of the bodega. |
| responsable | Usuario | User accountable for the bodega's operation. |
 
---
 
# MarketplaceWa
 
## Description
 
Represents a warehouse owned and operated directly by the Marketplace.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| operadorAsignado | Operador | Logistics operator responsible for daily operation. |
 
---
 
# SellersWa
 
## Description
 
Represents a warehouse owned and operated by a vendedor.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| vendedorPropietario | Vendedor | Vendedor who owns and operates the bodega. |
 
---
 
# Inventory
 
## Description
 
Represents the distributed stock of a product. It must always be linked to both a Producto and a Bodega, and negative stock is never permitted under any circumstance.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| producto | ProductoFisico | Product this inventory record belongs to. |
| bodega | Bodega | Warehouse where the stock is physically held. |
| cantidadDisponible | Integer | Units currently available for commercialization. |
| tipoMovimiento | MovementType | Ingreso, Reserva, Salida por venta, Ajuste, or Devolución. |
| fechaMovimiento | LocalDateTime | Date and time of the last inventory movement. |
 
---
 
# ShoppingCart
 
## Description
 
Represents the provisional selection of products made by a comprador before confirming a pedido. It is the initial state of the pedido lifecycle.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| comprador | Comprador | Owner of the carrito. |
| items | List\<Producto\> | Products provisionally selected. |
| fechaCreacion | LocalDateTime | Date and time the carrito was created. |
 
---
 
# Order
 
## Description
 
Represents the formal commercial commitment between a comprador and the Marketplace. It is the central entity of the system, progressing through a defined lifecycle: Carrito → Pendiente de Pago → Pagado → Despachado → Entregado/Finalizado. A finalized pedido cannot be modified under any circumstance.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| comprador | Comprador | Comprador who placed the pedido. |
| items | List\<Producto\> | Products included in the pedido. |
| estadoPedido | OrderStatus | Current stage of the pedido lifecycle. |
| fechaCreacion | LocalDateTime | Date and time the pedido was created. |
| fechaFinalizacion | LocalDateTime | Date and time the pedido was completed. |
 
---
 
# Invoice
 
## Description
 
Represents the commercial and financial information associated with a confirmed sale.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| pedido | Pedido | Pedido this factura is issued for. |
| montoTotal | BigDecimal | Total invoiced amount. |
| fechaEmision | LocalDateTime | Date and time the factura was issued. |
| estadoFactura | InvoiceStatus | Current status of the factura. |
 
---
 
# Shipping
 
## Description
 
Represents the logistics process — packing, dispatch, and transport — applied to pedidos that contain physical products.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| pedido | Pedido | Pedido being shipped. |
| bodegaOrigen | Bodega | Warehouse from which the shipment departs. |
| operador | Operador | Operator responsible for the dispatch. |
| estadoEnvio | ShipmentStatus | Current status of the shipment. |
| fechaDespacho | LocalDateTime | Date and time the shipment left the bodega. |
 
---
 
# Returns
 
## Description
 
Represents a post-sale process initiated when a comprador returns a delivered product. Subject to critical validations on the related inventory.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| pedido | Pedido | Pedido associated with the devolución. |
| motivo | String | Reason for the return. |
| estadoDevolucion | ReturnStatus | Current status of the devolución. |
| fechaSolicitud | LocalDateTime | Date and time the devolución was requested. |
 
---
 
# Refunds
 
## Description
 
Represents the financial reimbursement issued to a comprador as a result of an approved devolución.
 
## Attributes
 
| Attribute | Type | Description |
|-----------|------|-------------|
| devolucion | Devolucion | Devolución that originated the reembolso. |
| montoReembolsado | BigDecimal | Amount returned to the comprador. |
| estadoReembolso | RefundStatus | Current status of the reembolso. |
| fechaProcesamiento | LocalDateTime | Date and time the reembolso was processed. |
