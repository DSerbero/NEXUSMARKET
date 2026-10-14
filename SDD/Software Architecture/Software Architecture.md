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

El paquete Java raiz es `application`, dentro del modulo existente `nexusmarket`.

| Paquete | Responsabilidad |
|---|---|
| `application.domain.models` | User, Buyer, Seller, Product, Inventory, Cart, Order, Invoice, Shipment, Return, Refund y Warehouse. |
| `application.domain.enums` / `valueobjects` | Tipos controlados y catalogos del dominio. |
| `application.domain.exceptions` | Excepciones de validacion/reglas de negocio. |
| `application.domain.ports.in` | Contratos de entrada que exponen casos de uso. |
| `application.domain.ports.out` | Contratos de persistencia y servicios externos; implementados por adaptadores. |
| `application.domain.services` | Comportamiento de dominio reutilizable, independiente de infraestructura. |
| `application.adapters.useCases` | Servicios de aplicacion que coordinan validacion, dominio y output ports, agrupados por subdominio. |
| `application.adapters.rest` | Controladores, DTOs, mappers y manejo de errores HTTP. |
| `application.adapters.persistence.jpa` | Entidades, mappers y repositorios para persistencia relacional. |
| `application.adapters.persistence.mongodb` | Documentos, mappers y repositorios para persistencia documental. |
| `application.infrastructure` | Configuracion Spring, seguridad y notificaciones. |

Los nombres de subdominio deben reflejar NexusMarket: `user`, `authorization`, `buyer`, `seller`, `warehouse`, `product`, `inventory`, `cart`, `order`, `invoice`, `logistics`, `returns` y `reporting`. No se deben crear paquetes bancarios como `account` o `loan` para este proyecto.

## Estado observado

El codigo existente fue reubicado bajo `src/main/java/application`. Los modelos, catalogos, excepciones, output ports y servicios de aplicacion existentes ya tienen su paquete correspondiente. La carpeta `application.adapters.useCases` contiene esos servicios.

Todavia no hay implementaciones de puertos de persistencia JPA/MongoDB, entidades/documentos, controladores REST, DTOs, puertos de entrada ni adaptadores concretos de seguridad/notificacion. Sus dependencias Maven no equivalen a implementaciones. Esas capas deben agregarse al definir contratos de API y asignacion de cada agregado a SQL o MongoDB; no se deben inventar para hacer que la estructura parezca completa.

## Reglas de dependencia

1. El dominio solo importa Java y otros tipos del dominio.
2. Los output ports reciben/devuelven modelos del dominio, nunca entidades JPA, documentos MongoDB o DTOs.
3. Los adapters implementan los puertos; los servicios no consultan bases de datos directamente.
4. Los controllers validan formato/transporte y delegan; no aplican reglas de autorizacion ni mutan modelos.
5. Autenticacion verifica credenciales y produce una identidad; autorizacion valida rol, estado y propiedad del objeto de dominio.
6. `SUPERVISOR` tiene acceso de lectura, no de escritura.

## Decisiones de negocio pendientes en el SDD

Estos puntos aparecen como supuestos en los documentos, no como reglas confirmadas: un solo carrito activo por buyer; posibilidad de volver a solicitar una devolucion rechazada; reembolsos siempre totales frente a parciales; y alcance de los operadores logisticos al aprobar/completar devoluciones. Deben confirmarse antes de implementar persistencia o workflows completos.
