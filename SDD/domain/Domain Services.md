# Services

## Introduction

Este documento presenta una visión conceptual de los servicios que componen el sistema NexusMarket.

Los servicios aquí descritos definen las principales capacidades de negocio expuestas por el sistema. En este nivel, cada servicio se describe únicamente en términos de su propósito y responsabilidad dentro del dominio.

La definición detallada de cada servicio —incluyendo entradas, salidas, reglas de negocio, validaciones, requisitos de autorización, interacciones con el dominio, excepciones y consideraciones de persistencia— se documentará en archivos separados organizados por **subdominio**.

La documentación de servicios se divide conceptualmente en los siguientes subdominios, alineados con los objetivos funcionales OBJ-01 a OBJ-12 de la especificación:

- **User and Authentication Management**
- **Seller Management**
- **Buyer Management**
- **Warehouse Management**
- **Product Catalog Management**
- **Inventory Management**
- **Cart Management**
- **Order Management**
- **Invoicing Management**
- **Logistics Management**
- **Returns and Refunds Management**
- **Administrative Reporting**
- **Authorization**

---

# User and Authentication Management Services

## Login

Autentica a un usuario del sistema mediante sus credenciales registradas y establece una sesión autenticada.

## Logout

Termina la sesión autenticada de un usuario y evita que se realicen más operaciones a través de esa sesión.

## Consult User

Recupera la información de un usuario de acuerdo con los permisos del usuario solicitante.

## Update User

Actualiza la información mantenida para un usuario existente según las reglas de negocio aplicables.

## Change User Status

Cambia el estado operativo de un usuario, como activar, desactivar o bloquear su acceso al sistema.

---

# Seller Management Services

## Register Seller

Crea un nuevo seller y su bodega inicial, incorporándolo al Marketplace. Restringido al rol Admin, dado que los sellers no pueden autoregistrarse.

## Consult Seller

Recupera la información de un seller de acuerdo con los permisos del usuario solicitante.

## Update Seller

Actualiza la información mantenida para un seller existente.

## Consult Seller Catalog

Recupera los productos y bodegas asociados a un seller.

---

# Buyer Management Services

## Register Buyer

Crea un nuevo buyer y establece su estado comercial inicial e información de entrega.

## Consult Buyer

Recupera la información de un buyer de acuerdo con los permisos del usuario solicitante.

## Update Buyer

Actualiza la información mantenida para un buyer existente, incluyendo sus direcciones de entrega.

## Change Buyer Commercial Status

Cambia el estado comercial de un buyer, habilitándolo o suspendiéndolo para realizar compras.

---

# Warehouse Management Services

## Register Warehouse

Crea una nueva warehouse y la asocia al Marketplace o a un seller.

## Consult Warehouse

Recupera la información de una warehouse de acuerdo con los permisos del usuario solicitante.

## Assign Logistics Operator

Asigna un operador logístico como responsable de la operación diaria de una warehouse.

## Update Warehouse

Actualiza la información mantenida para una warehouse existente.

---

# Product Catalog Management Services

## Register Product

Crea un nuevo producto (físico o digital) y lo asocia al catálogo de un seller.

## Consult Product

Recupera la información de un producto de acuerdo con los permisos del usuario solicitante.

## Update Product

Actualiza la información mantenida para un producto existente, como sus variantes o descripción.

## Publish Product

Cambia el estado de un producto a publicado, haciéndolo visible en el catálogo público.

## Suspend Product

Cambia el estado de un producto a suspendido, ocultándolo temporalmente del catálogo público.

## Discontinue Product

Retira de forma permanente un producto de la venta.

---

# Inventory Management Services

## Register Initial Stock

Registra la cantidad inicial disponible de un producto físico en una warehouse específica.

## Consult Inventory

Recupera el registro de inventario asociado a un producto y una warehouse.

## Reserve Stock

Reserva la cantidad de un producto físico necesaria para atender un order pendiente, garantizando que no se sobrevenda el stock.

## Release Reserved Stock

Libera stock previamente reservado y lo devuelve al inventario disponible, por ejemplo cuando un order se cancela.

## Register Stock Movement

Registra un movimiento de inventario (Stock In, Sale Outbound, Adjustment o Return Inbound) sobre un registro de inventario.

---

# Cart Management Services

## Add Product to Cart

Agrega un producto al cart de un buyer como selección provisional.

## Remove Product from Cart

Elimina un producto previamente seleccionado del cart de un buyer.

## Consult Cart

Recupera el contenido actual del cart de un buyer.

## Confirm Cart

Convierte el contenido del cart de un buyer en un order formal, iniciando el ciclo de vida del pedido.

---

# Order Management Services

## Create Order

Crea un order formal a partir del cart confirmado de un buyer y establece su estado inicial de pago pendiente.

## Consult Order

Recupera la información de un order de acuerdo con los permisos del usuario solicitante.

## Confirm Order Payment

Confirma el pago de un order pendiente y lo avanza al estado pagado, iniciando el proceso de alistamiento.

## Dispatch Order

Marca un order como despachado una vez que ha salido físicamente de la warehouse.

## Finalize Order

Marca un order como entregado y finalizado tras la confirmación de entrega, dejándolo inmutable.

---

# Invoicing Management Services

## Issue Invoice

Genera un invoice para un order confirmado, registrando la información comercial y financiera de la venta.

## Consult Invoice

Recupera la información de un invoice de acuerdo con los permisos del usuario solicitante.

## Void Invoice

Anula un invoice previamente emitido según las reglas de negocio aplicables.

---

# Logistics Management Services

## Register Shipment

Crea el registro de un shipment para un order que contiene productos físicos, vinculándolo a la warehouse de origen y al operador responsable.

## Update Shipment Status

Actualiza el estado de un shipment a medida que avanza por las etapas de preparación, despacho, tránsito y entrega.

## Consult Shipment

Recupera la información de un shipment de acuerdo con los permisos del usuario solicitante.

---

# Returns and Refunds Management Services

## Request Return

Registra la solicitud de un buyer para devolver un producto ya entregado e inicia el ciclo de vida del return.

## Approve Return

Aprueba una solicitud de return tras validar las condiciones del inventario y del order relacionados.

## Reject Return

Rechaza una solicitud de return y registra la decisión correspondiente.

## Complete Return

Finaliza un return aprobado reintegrando el producto devuelto al inventario.

## Process Refund

Emite un reembolso financiero al buyer como resultado de un return aprobado y completado.

## Consult Refund

Recupera la información de un refund de acuerdo con los permisos del usuario solicitante.

---

# Administrative Reporting Services

## Consult Administrative Reports

Recupera información administrativa consolidada sobre usuarios, sellers, inventario, orders y returns con fines de supervisión.

## Consult Responsibility Matrix

Recupera las operaciones que un rol determinado está autorizado a realizar, según la matriz de responsabilidades del sistema.

---

# Authorization Services

## Validate Permissions

Determina si un usuario tiene permiso para realizar una operación de negocio específica, según su rol y estado (RG-02).

## Validate Role Scope

Determina si un usuario está intentando gestionar información fuera del alcance de su rol asignado (RG-03).

## Validate Authenticated Session

Determina si el usuario solicitante está autenticado antes de permitir que cualquier operación proceda (RG-01).

---

# Service Organization

Los servicios descritos en este documento constituyen el **catálogo de servicios de alto nivel** del sistema.

Intencionalmente no describen detalles de implementación ni flujos de negocio completos.

Las especificaciones detalladas se mantendrán en archivos Markdown separados, organizados por subdominio. Por ejemplo:

```text
services/
│   └── user-authentication-services.md
│
│   └── seller-services.md
│
│   └── buyer-services.md
│
│   └── warehouse-services.md
│
│   └── catalog-services.md
│
│   └── inventory-services.md
│
│   └── cart-services.md
│
│   └── order-services.md
│
│   └── invoicing-services.md
│
│   └── logistics-services.md
│
│   └── returns-refunds-services.md
│
│   └── administrative-reporting-services.md
│
    └── authorization-services.md
```