# Domain Value Objects

## Introduction

Value Objects represent immutable concepts within the NexusMarket domain. Unlike Entities, they do not have their own identity; instead, they are defined entirely by their attributes.

These objects encapsulate controlled business values, improve domain expressiveness, and prevent the use of primitive types or scattered string literals throughout the application.

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

Represents a generic business catalog used throughout the NexusMarket domain.

All controlled business values inherit from this class, ensuring a consistent structure across the application.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| code | String | Unique business identifier. |
| name | String | Human-readable name displayed within the application. |
| description | String | Business definition of the catalog value. |

---

# UserRole

## Description

Represents the responsibilities and permissions assigned to a system user.

Each user has exactly one role, and roles determine which business operations a user may perform (RG-02, RG-03).

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| BUYER | Buyer | Purchases products published on the Marketplace. |
| SELLER | Seller | Registers and manages products; onboarded only by an Admin. |
| LOGISTICS_OPERATOR | Logistics Operator | Handles physical operation of warehouses and dispatch. |
| ADMIN | Admin | Manages sellers and warehouses. |
| SUPERVISOR | Supervisor | Read-only profile for consultation and monitoring. |

---

# UserStatus

## Description

Represents the operational condition of a user within the Marketplace.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| ACTIVE | Active | User can access and operate on the system normally. |
| BLOCKED | Blocked | User access has been suspended. |
| INACTIVE | Inactive | User exists but is not currently operating on the platform. |

---

# BuyerStatus

## Description

Represents the commercial condition of a buyer for making purchases.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| ENABLED | Enabled | Buyer can place orders normally. |
| SUSPENDED | Suspended | Buyer is temporarily restricted from purchasing. |

---

# ProductType

## Description

Represents whether a product is physical or digital, determining whether it requires inventory and dispatch.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| PHYSICAL | Physical | Requires inventory tracking and dispatch. |
| DIGITAL | Digital | Delivered immediately after payment confirmation. |

---

# ProductStatus

## Description

Represents the visibility state of a product in the catalog.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| PUBLISHED | Published | Product is visible in the public catalog. |
| SUSPENDED | Suspended | Product is temporarily hidden from the catalog. |
| DISCONTINUED | Discontinued | Product is permanently removed from sale. |

---

# MovementType

## Description

Represents the type of movement applied to an inventory record.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| STOCK_IN | Stock In | Stock added to the warehouse. |
| RESERVATION | Reservation | Stock reserved for a pending order. |
| SALE_OUTBOUND | Sale Outbound | Stock removed due to a confirmed sale. |
| ADJUSTMENT | Adjustment | Manual correction of stock quantity. |
| RETURN_INBOUND | Return Inbound | Stock returned after a return. |

---

# OrderStatus

## Description

Represents the current stage of an order within its lifecycle. An order in a final state cannot be modified under any circumstance.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| CART | Cart | Provisional product selection. |
| PENDING_PAYMENT | Pending Payment | Awaiting financial confirmation. |
| PAID | Paid | Payment confirmed; preparation begins. |
| DISPATCHED | Dispatched | Order has physically left the warehouse. |
| DELIVERED_FINALIZED | Delivered / Finalized | Delivery confirmed; order lifecycle complete. |

---

# InvoiceStatus

## Description

Represents the current status of an invoice issued for an order.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| ISSUED | Issued | Invoice has been generated. |
| PAID | Paid | Invoice amount has been collected. |
| VOIDED | Voided | Invoice has been cancelled. |

---

# ShipmentStatus

## Description

Represents the current status of a shipment for a physical order.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| IN_PREPARATION | In Preparation | Order is being packed at the warehouse. |
| DISPATCHED | Dispatched | Shipment has left the warehouse. |
| IN_TRANSIT | In Transit | Shipment is being transported. |
| DELIVERED | Delivered | Shipment reached the buyer. |

---

# ReturnStatus

## Description

Represents the current status of a return requested by a buyer.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| REQUESTED | Requested | Return has been submitted. |
| APPROVED | Approved | Return has been validated and accepted. |
| REJECTED | Rejected | Return request was denied. |
| COMPLETED | Completed | Returned product has been processed back into inventory. |

---

# RefundStatus

## Description

Represents the current status of a refund issued after an approved return.

## Inherits From

DomainCatalog

## Allowed Values

| Code | Name | Description |
|------|------|-------------|
| PENDING | Pending | Refund has been requested but not yet processed. |
| PROCESSED | Processed | Funds have been returned to the buyer. |
| REJECTED | Rejected | Refund request was denied. |

---

# Primitive Enumerations

The following concepts are simple enumerations because they represent fixed technical values without business behavior.

---

## AddressType

### Description

Represents the classification of a delivery address registered by a buyer.

### Values

- PRIMARY
- ADDITIONAL

---

## NotificationChannel

### Description

Represents the communication channel used by the system to notify participants about order and shipment events.

### Values

- EMAIL
- SMS
- PUSH_NOTIFICATION

---

# Design Notes

- All business catalogs inherit from **DomainCatalog**.
- Value Objects are immutable.
- Equality is determined by their values rather than object identity.
- Business entities reference Value Objects instead of primitive strings.
- Primitive Enumerations are reserved exclusively for technical concepts that do not encapsulate business rules or behavior.
- This approach improves maintainability, consistency, and alignment with Domain-Driven Design (DDD) principles while supporting future domain evolution.
