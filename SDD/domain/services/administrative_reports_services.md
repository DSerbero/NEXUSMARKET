# Administrative Reporting Services

## 1. Introduction

Este documento define los servicios del subdominio **Administrative Reporting** del sistema NexusMarket.

El subdominio es responsable de exponer información consolidada de solo lectura para fines de supervisión y soporte administrativo, sin ejecutar ni modificar ninguna operación de negocio de los demás subdominios.

Las principales capacidades de negocio son:

* Consult Administrative Reports.
* Consult Responsibility Matrix.

Este subdominio es distinto de todos los anteriores en un aspecto fundamental: **no introduce Domain Models propios**. Opera exclusivamente sobre datos ya existentes en otros subdominios (`User`, `Seller`, `Buyer`, `Order`, `Inventory`, `Return`, etc.), agregándolos con fines de lectura.

```text
Administrative Reporting
        |
        v
  Solo lectura agregada sobre:
  User, Seller, Buyer, Product, Inventory,
  Order, Return, Refund, Shipment
```

---

# 2. Domain Model Context

## 2.1 No Dedicated Domain Model

A diferencia de todos los subdominios anteriores, este no define una entidad nueva (no existe un `Report` o `AdministrativeReport` como Domain Model persistente). Los "reportes" son **proyecciones de lectura** (read models) construidos a partir de los Output Ports de los demás subdominios — no datos que este subdominio posea o modifique.

## 2.2 Relation to Other Subdomains

```text
Administrative Reporting
        |
        ├── UserRepositoryPort
        ├── SellerRepositoryPort
        ├── BuyerRepositoryPort
        ├── ProductRepositoryPort
        ├── InventoryRepositoryPort
        ├── OrderRepositoryPort
        ├── ReturnRepositoryPort
        └── RefundRepositoryPort
```

Este subdominio consume los mismos Output Ports ya definidos por los subdominios dueños de cada entidad — no debe crear sus propios puertos de escritura, ni una copia paralela de los datos, porque eso introduciría una fuente de verdad duplicada.

---

# 3. Supervisor and Administrative Reporting Relationship

```text
Requesting User
 |
 ├── SUPERVISOR  -> acceso completo de lectura, sin restricción
 └── ADMIN        -> acceso completo de lectura, sin restricción
```

Ningún otro rol (`BUYER`, `SELLER`, `LOGISTICS_OPERATOR`) tiene acceso a este subdominio — los reportes administrativos, por definición, exceden el alcance de visibilidad de esos roles (RG-03).

---

# 4. Service Design Principle

A diferencia de los subdominios anteriores, aquí no hay comportamiento de dominio que ejecutar ni invariantes que proteger — el "servicio cohesivo" consiste en agregar y presentar datos consistentes, sin mutar nada.

---

# 5. Standard Application Service Pattern

```text
Input Query Parameters (filtros, rango de fechas, etc.)
                |
                v
Validate requesting User (ADMIN o SUPERVISOR)
                |
                v
Retrieve data through multiple Output Ports (solo lectura)
                |
                v
Aggregate into a read model
                |
                v
Return result
```

No hay pasos de "Execute Domain behavior" ni "Persist" — es la única familia de servicios de todo el sistema que no escribe nada.

---

# 6. Input Contract

Incorrecto:

```java
consultAdministrativeReports(String type); // string mágico sin estructura
```

Preferido:

```java
consultAdministrativeReports(
    User requestingUser,
    ReportFilter filter   // rango de fechas, subdominio, etc., como Value Object
);
```

---

# 7. Authoritative State

Cada dato agregado proviene siempre del Output Port del subdominio dueño — este subdominio nunca debe mantener una copia propia que pueda desincronizarse del estado real.

---

# 8. External Information

Todos los datos de este subdominio son, por definición, "información externa" desde su propia perspectiva — se obtienen exclusivamente a través de los Output Ports ya definidos en los demás subdominios (§2.2). Nunca accede directamente a la base de datos.

---

# 9. Report Validation

* `requestingUser.role in {ADMIN, SUPERVISOR}`.
* Los filtros de fecha/rango, si se proporcionan, son coherentes (`fechaInicio <= fechaFin`).

---

# 10. Domain Behavior

No aplica — este subdominio no muta ningún Domain Model. Su única "operación" es consultar y agregar.

---

# 11. Input Ports

```text
ConsultAdministrativeReportsUseCase
ConsultResponsibilityMatrixUseCase
```

---

# 12. Output Ports

Este subdominio no define Output Ports propios. Reutiliza los ya existentes:

```text
UserRepositoryPort
SellerRepositoryPort
BuyerRepositoryPort
ProductRepositoryPort
InventoryRepositoryPort
OrderRepositoryPort
ReturnRepositoryPort
RefundRepositoryPort
```

## Service-to-Port Matrix

| Service | UserRepositoryPort | SellerRepositoryPort | OrderRepositoryPort | InventoryRepositoryPort | ReturnRepositoryPort |
|---|---:|---:|---:|---:|---:|
| Consult Administrative Reports | ✓ (read) | ✓ (read) | ✓ (read) | ✓ (read) | ✓ (read) |
| Consult Responsibility Matrix | | | | | |

`Consult Responsibility Matrix` no consulta datos persistidos de negocio — expone la matriz de roles y permisos ya definida en `Authorization Services` (§System Roles, §Role Authorization Rules de ese documento), que es información estática de configuración, no un Domain Model.

---

# 13. Consult Administrative Reports

## 13.1 Purpose

Recupera información administrativa consolidada sobre usuarios, sellers, inventario, orders y returns con fines de supervisión.

## 13.2 Input

```text
User (requestingUser)
ReportFilter (opcional: rango de fechas, subdominio específico)
```

## 13.3 Validations

* `requestingUser.role in {ADMIN, SUPERVISOR}`.
* Filtros de fecha coherentes, si se proporcionan.

## 13.4 Processing

```text
1. Validate requesting User.
2. Query relevant Output Ports according to the requested scope.
3. Aggregate results into a read-only report structure.
4. Return aggregated report.
```

## 13.5 Scope Restriction

Este servicio no reemplaza las consultas específicas de cada subdominio (`Consult Order`, `Consult Inventory`, etc.) — es un agregado de alto nivel para visión general (p. ej., "total de pedidos este mes", "sellers registrados este trimestre"), no un mecanismo para eludir las validaciones de autorización granular de cada subdominio individual.

---

# 14. Consult Responsibility Matrix

## 14.1 Purpose

Recupera las operaciones que un rol determinado está autorizado a realizar, según la matriz de responsabilidades del sistema.

## 14.2 Input

```text
User (requestingUser)
UserRole (rol a consultar, opcional — si se omite, se devuelve la matriz completa)
```

## 14.3 Validations

* `requestingUser.role in {ADMIN, SUPERVISOR}`.

## 14.4 Processing

Este servicio no consulta ningún Output Port de persistencia — su fuente de datos es la definición estática de `Role Authorization Rules` (`Authorization Services`, sección homónima). Es, en efecto, una vista de solo lectura sobre reglas de configuración, no sobre datos transaccionales.

```text
1. Validate requesting User.
2. Retrieve static role-permission definitions.
3. Return matrix (completa o filtrada por rol).
```

---

# 15. Exception Model

```text
UnauthorizedReportAccessException
InvalidReportFilterException
```

Dado que este subdominio no muta estado, su catálogo de excepciones es intencionalmente mínimo — no hay excepciones de transición de estado, unicidad, ni invariantes de dominio que proteger.

---

# 16. Persistence Boundary

```text
AdministrativeReportingService
        |
        v
Output Ports de otros subdominios (solo lectura)
        |
        v
Persistence Adapters existentes
        |
        v
Database
```

Este subdominio no tiene su propio adaptador de persistencia — no persiste nada.

---

# 17. Controller Responsibilities

Los controllers no deben implementar:

* Lógica de agregación de datos.
* Reglas de autorización.

---

# 18. Validation Matrix

| Validation | Consult Administrative Reports | Consult Responsibility Matrix |
|---|---:|---:|
| Requesting User | Yes | Yes |
| Role (ADMIN/SUPERVISOR) | Yes | Yes |
| Filter coherence | Yes (si se proporciona) | N/A |

---

# 19. Business Rules Summary

## BR-001 — Read-Only Subdomain

Este subdominio nunca escribe ni muta Domain Models de otros subdominios.

## BR-002 — No Duplicated Source of Truth

Los datos siempre se obtienen en tiempo real de los Output Ports existentes — no se mantiene una copia propia.

## BR-003 — Restricted to ADMIN and SUPERVISOR

Ningún otro rol tiene acceso a este subdominio.

## BR-004 — Responsibility Matrix Is Static Configuration, Not Domain Data

---

# 20. Anti-Patterns

## 20.1 Maintaining a Duplicated Read Model in Its Own Database

Inválido: sincronizar una copia propia de `Order`, `Inventory`, etc., en una tabla/colección separada "para reportes" — introduce una fuente de verdad paralela que puede desincronizarse.

## 20.2 Using This Subdomain to Bypass Granular Authorization

Inválido: que un `SELLER` use `Consult Administrative Reports` para ver inventario de otro seller, evadiendo las restricciones de propiedad ya definidas en `Inventory Management` y `Authorization Services`.

## 20.3 Direct Database Access

## 20.4 Mutating State From a "Reporting" Service

Inválido: cualquier operación de escritura disfrazada de "reporte".

---

# 21. Testing Requirements

## 21.1 Consult Administrative Reports Tests

* Admin/Supervisor consulta reportes exitosamente.
* Seller/Buyer/LogisticsOperator intentan acceder (rechazado).
* Filtro de fechas incoherente (`fechaInicio > fechaFin`) (rechazado).

## 21.2 Consult Responsibility Matrix Tests

* Admin/Supervisor consulta la matriz completa.
* Admin/Supervisor consulta la matriz filtrada por un rol específico.
* Otro rol intenta acceder (rechazado).

---

# 22. Definition of Done

* Representa una operación de negocio coherente de solo lectura.
* Recibe los Domain Models y Value Objects apropiados.
* No expone DTOs REST como contratos de aplicación.
* Consulta siempre los Output Ports de los subdominios dueños de cada dato, sin duplicar la fuente de verdad.
* Restringe el acceso a `ADMIN` y `SUPERVISOR`.
* No muta ningún Domain Model.
* Puede probarse sin infraestructura.

---

# 23. Final Service Catalog

```text
Administrative Reporting
|
+-- Consult Administrative Reports
|
+-- Consult Responsibility Matrix
```

---

# 24. Final Design Rule

> **Cada servicio de aplicación de este subdominio es una operación de solo lectura responsable de agregar información ya existente en otros subdominios a través de sus Output Ports, sin duplicar la fuente de verdad, sin mutar ningún Domain Model, y restringida exclusivamente a los roles ADMIN y SUPERVISOR.**