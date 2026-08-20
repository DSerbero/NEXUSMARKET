# Domain Model

## Introduction

The Domain Model represents the core business entities of the NexusMarket Marketplace System. These entities encapsulate the business rules, data, and relationships described in the functional business specification.

The model follows Object-Oriented Design principles and applies inheritance to eliminate duplicated information while promoting reusability and maintainability.

---

# Domain Class Hierarchy

```text
User (Abstract)
├── Buyer
├── Seller
├── LogisticsOperator
├── Admin
└── Supervisor

Product

Warehouse (Abstract)
├── MarketplaceWarehouse
└── SellerWarehouse

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

# User (Abstract)

## Description

Represents any person authorized to interact with the NexusMarket system. This abstract class centralizes all common identification and access information shared by every role in the platform (Buyer, Seller, LogisticsOperator, Admin, Supervisor).

Each user has exactly one role and can only manage information within the scope of that role (business rules RG-02, RG-03).

This class cannot be instantiated directly.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the user across the platform. |
| fullName | String | Official name of the user. |
| email | String | Primary access and communication channel. Must be unique. |
| role | UserRole | Defines the responsibilities and permissions of the user. Unique per user. |
| status | UserStatus | Operational condition of the user (Active, Blocked, etc.). |

---

# Buyer

## Description

Represents a customer who purchases products published on the Marketplace.

A buyer never manages information belonging to other buyers nor inventory data.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| primaryAddress | String | Usual location for deliveries. |
| additionalAddresses | List\<String\> | Secondary delivery locations. |
| buyerStatus | BuyerStatus | Condition of the buyer for making purchases. |

---

# Seller

## Description

Represents a party responsible for registering and managing products on the Marketplace.

Sellers cannot self-register; they are onboarded exclusively by an Admin.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| associatedWarehouses | List\<Warehouse\> | Warehouses linked to the seller's operation. |
| productCatalog | List\<Product\> | Products registered and managed by the seller. |

---

# LogisticsOperator

## Description

Represents the party in charge of the physical operation of warehouses and order dispatch.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| assignedWarehouse | Warehouse | Warehouse the operator is responsible for. |

---

# Admin

## Description

Represents the administrator responsible for managing sellers and warehouses across the Marketplace.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| administrativePermissions | List\<String\> | Administrative capabilities granted to the account. |

---

# Supervisor

## Description

Represents a read-only profile used for consultation and operational monitoring across the system.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| consultationScope | String | Scope of information the supervisor is authorized to view. |

---

# Product (Abstract)

## Description

Represents any good offered for sale on the Marketplace, either physical or digital.

This class cannot be instantiated directly.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| productType | ProductType | Physical or Digital. |
| variants | List\<String\> | Differences such as color, size, model, etc. |
| status | ProductStatus | Published, Suspended, or Discontinued. |
| seller | Seller | Seller who owns and manages the product. |

---

# PhysicalProduct

## Description

Represents a tangible product that requires inventory tracking and physical dispatch through a warehouse.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| associatedInventory | Inventory | Stock record linking the product to a warehouse. |
| requiresShipment | Boolean | Indicates the product must go through the logistics flow. |

---

# DigitalProduct

## Description

Represents an intangible product delivered immediately upon payment confirmation, without involving a warehouse.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| immediateDelivery | Boolean | Confirms the product is delivered right after payment. |
| digitalAsset | String | Reference to the digital asset delivered to the buyer. |

---

# Warehouse 

## Description

Represents a physical location where inventory is stored and managed.

Warehouses are classified according to who administers them: the Marketplace itself or a seller.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| identifier | String | Uniquely identifies the warehouse. |
| location | String | Physical location of the warehouse. |
| responsibleUser | User | User accountable for the warehouse's operation. |

---

# Inventory

## Description

Represents the distributed stock of a product. It must always be linked to both a Product and a Warehouse, and negative stock is never permitted under any circumstance.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| product | PhysicalProduct | Product this inventory record belongs to. |
| warehouse | Warehouse | Warehouse where the stock is physically held. |
| availableQuantity | Integer | Units currently available for commercialization. |
| movementType | MovementType | Stock In, Reservation, Sale Outbound, Adjustment, or Return Inbound. |
| movementDate | LocalDateTime | Date and time of the last inventory movement. |

---

# Cart

## Description

Represents the provisional selection of products made by a buyer before confirming an order. It is the initial state of the order lifecycle.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| buyer | Buyer | Owner of the cart. |
| items | List\<Product\> | Products provisionally selected. |
| creationDate | LocalDateTime | Date and time the cart was created. |

---

# Order

## Description

Represents the formal commercial commitment between a buyer and the Marketplace. It is the central entity of the system, progressing through a defined lifecycle: Cart → Pending Payment → Paid → Dispatched → Delivered/Finalized. A finalized order cannot be modified under any circumstance.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| buyer | Buyer | Buyer who placed the order. |
| items | List\<Product\> | Products included in the order. |
| orderStatus | OrderStatus | Current stage of the order lifecycle. |
| creationDate | LocalDateTime | Date and time the order was created. |
| completionDate | LocalDateTime | Date and time the order was completed. |

---

# Invoice

## Description

Represents the commercial and financial information associated with a confirmed sale.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| order | Order | Order this invoice is issued for. |
| totalAmount | BigDecimal | Total invoiced amount. |
| issueDate | LocalDateTime | Date and time the invoice was issued. |
| invoiceStatus | InvoiceStatus | Current status of the invoice. |

---

# Shipment

## Description

Represents the logistics process — packing, dispatch, and transport — applied to orders that contain physical products.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| order | Order | Order being shipped. |
| originWarehouse | Warehouse | Warehouse from which the shipment departs. |
| operator | LogisticsOperator | Operator responsible for the dispatch. |
| shipmentStatus | ShipmentStatus | Current status of the shipment. |
| dispatchDate | LocalDateTime | Date and time the shipment left the warehouse. |

---

# Return

## Description

Represents a post-sale process initiated when a buyer returns a delivered product. Subject to critical validations on the related inventory.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| order | Order | Order associated with the return. |
| reason | String | Reason for the return. |
| returnStatus | ReturnStatus | Current status of the return. |
| requestDate | LocalDateTime | Date and time the return was requested. |

---

# Refund

## Description

Represents the financial reimbursement issued to a buyer as a result of an approved return.

## Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| relatedReturn | Return | Return that originated the refund. |
| refundedAmount | BigDecimal | Amount returned to the buyer. |
| refundStatus | RefundStatus | Current status of the refund. |
| processingDate | LocalDateTime | Date and time the refund was processed. |
