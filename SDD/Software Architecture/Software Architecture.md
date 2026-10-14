# Arquitectura de Software - NexusMarket

## Estilo arquitectonico

NexusMarket sigue arquitectura hexagonal (Ports and Adapters) con un dominio de marketplace. La estructura del ejemplo bancario se usa solo como referencia de capas; no se trasladan entidades ni reglas propias de banca.

```text
REST Controllers
	|
	v
Input Ports / Use Cases
	|
	v
Domain Models + Domain Services
	|
	v
Output Ports
	|
	v
Persistence / Security / Notification Adapters
```

Las dependencias apuntan hacia el dominio. Los modelos del dominio no dependen de Spring, JPA, MongoDB ni DTOs REST. Los casos de uso coordinan reglas de negocio y puertos; los adaptadores traducen entre protocolos o persistencia y contratos de aplicacion/dominio.

## Mapeo de paquetes

El paquete Java raiz es `app`, dentro del modulo existente `nexusmarket`.

| Paquete | Responsabilidad |
|---|---|
| `app.domain.models` | User, Buyer, Seller, Product, Inventory, Cart, Order, Invoice, Shipment, Return, Refund y Warehouse. |
| `app.domain.enums` / `valueobjects` | Tipos controlados y catalogos del dominio. |
| `app.domain.exceptions` | Excepciones de validacion/reglas de negocio. |
| `app.domain.ports.in` | Contratos de entrada que exponen casos de uso, agrupados por actor cuando corresponda. |
| `app.domain.ports.out` | Contratos de persistencia y servicios externos; implementados por adaptadores. |
| `app.domain.services` | Servicios de negocio organizados por subdominio; aplican reglas y usan output ports, nunca adaptadores concretos. |
| `app.adapters.useCases` | Adaptadores de entrada por actor/rol; implementan input ports y delegan en servicios de dominio. |
| `app.adapters.rest` | Controladores, DTOs, mappers y manejo de errores HTTP; los controladores invocan input ports. |
| `app.adapters.persistence.jpa` | Entidades, mappers y repositorios para persistencia relacional. |
| `app.adapters.persistence.mongodb` | Documentos, mappers y repositorios para persistencia documental. |
| `app.infrastructure` | Configuracion Spring, seguridad y notificaciones. |

Los nombres de subdominio deben reflejar NexusMarket: `user`, `authorization`, `buyer`, `seller`, `warehouse`, `product`, `inventory`, `cart`, `order`, `invoice`, `logistics`, `returns` y `reporting`. No se deben crear paquetes bancarios como `account` o `loan` para este proyecto.

## Estado observado

El codigo existente usa `src/main/java/app`. Los servicios de dominio se organizan por subdominio en `app.domain.services`; `DomainValidations` contiene validaciones reutilizables y las clases de cada subdominio implementan operaciones de negocio mediante output ports.

El flujo expuesto actualmente es `ProductController -> PublicAccessPort -> PublicAccessUseCaseImpl -> ProductService`. El adaptador `PublicAccessUseCaseImpl` se mantiene delgado y no contiene reglas de producto. Los demas servicios ya pueden probarse de forma aislada, pero sus input ports y adaptadores por rol se agregaran cuando se expongan sus contratos de API; no se deben inventar endpoints para completar el arbol.

La capa REST ya contiene `ProductController`, y MongoDB tiene un adaptador concreto para productos. Aun faltan los adaptadores JPA y las implementaciones de persistencia para los demas agregados. Las dependencias Maven por si solas no equivalen a esas implementaciones.

La asignacion de `User`, `Buyer`, `Cart` y `Order` a MySQL, la permanencia de `Product` en MongoDB y las reglas de consistencia estan definidas en [Persistencia y Consistencia](Persistence%20and%20Consistency.md). Esa decision es normativa para los proximos adaptadores; no significa que ya esten implementados.

## Correspondencia con el proyecto bancario

Se conserva la separacion de responsabilidades usada en el proyecto bancario:

| Capa | NexusMarket | Responsabilidad |
|---|---|---|
| Servicios | `app.domain.services.<subdomain>` | Ejecutan una capacidad de negocio y dependen de modelos y output ports. |
| Puertos de entrada | `app.domain.ports.in` | Definen las operaciones disponibles para cada actor/rol. |
| Adaptadores de casos de uso | `app.adapters.useCases` | Implementan los puertos de entrada y delegan en servicios; no duplican reglas. |

Los servicios se agrupan por subdominio de Marketplace, no por conceptos bancarios. Las operaciones de devolucion y reembolso permanecen en servicios separados porque tienen ciclos y responsabilidades diferentes.

## Reglas de dependencia

1. Los modelos, value objects y excepciones no dependen de frameworks ni adaptadores. Los servicios dependen de modelos y contratos de puertos, no de infraestructura concreta.
2. Los output ports reciben/devuelven modelos del dominio, nunca entidades JPA, documentos MongoDB o DTOs.
3. Los adapters implementan los puertos; los servicios no consultan bases de datos directamente.
4. Los adaptadores de casos de uso implementan input ports y delegan; no duplican reglas de negocio.
5. Los controllers validan formato/transporte e invocan input ports; no aplican reglas de autorizacion ni mutan modelos.
6. Autenticacion verifica credenciales y produce una identidad; autorizacion valida rol, estado y propiedad del objeto de dominio.
7. `SUPERVISOR` tiene acceso de lectura, no de escritura.

## Decisiones de negocio pendientes en el SDD

Estos puntos siguen abiertos en los documentos: posibilidad de volver a solicitar una devolucion rechazada; reembolsos siempre totales frente a parciales; alcance de los operadores logisticos al aprobar/completar devoluciones; moneda del marketplace; y si los items de carrito/pedido requieren cantidad explicita. La regla de un solo carrito activo por buyer ya se decidio y se refleja en la persistencia relacional.
