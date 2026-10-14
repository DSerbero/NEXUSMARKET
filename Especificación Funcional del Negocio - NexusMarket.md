# **ESPECIFICACIÓN FUNCIONAL: NEXUSMARKET**

# **CONTEXTO E INTRODUCCIÓN**

## **1\. Introducción y Contexto**

NexusMarket es una plataforma digital centralizada que actúa como intermediario comercial entre compradores y vendedores. El sistema administra integralmente la operación, desde el registro de usuarios y publicación de productos hasta la logística, facturación y posventa, garantizando trazabilidad y coordinación entre todos los participantes.

## **2\. Objetivos y Alcance**

El sistema deberá permitir el cumplimiento de los siguientes objetivos estratégicos:

| Código | Objetivo Funcional |
| :---- | :---- |
| OBJ-01 | Administrar la información de todos los usuarios del Marketplace. |
| OBJ-02 | Gestionar el registro y administración de vendedores. |
| OBJ-03 | Administrar compradores registrados. |
| OBJ-04 | Controlar la información de las bodegas. |
| OBJ-05 | Gestionar el catálogo de productos. |
| OBJ-06 | Administrar el inventario distribuido. |
| OBJ-07 | Gestionar el carrito de compras. |
| OBJ-08 | Controlar el ciclo completo de los pedidos. |
| OBJ-09 | Administrar la facturación de las compras. |
| OBJ-10 | Gestionar los procesos logísticos. |
| OBJ-11 | Administrar devoluciones y reembolsos. |
| OBJ-12 | Consolidar información administrativa para consulta. |

## **3\. Alcance del Sistema**

El sistema administrará exclusivamente los procesos descritos en esta especificación funcional.

### **3.1 Procesos Incluidos**

| Proceso | Estatus |
| :---- | :---- |
| Registro de compradores | ✔ Incluido |
| Administración de usuarios | ✔ Incluido |
| Registro administrativo de vendedores | ✔ Incluido |
| Administración de bodegas | ✔ Incluido |
| Gestión del catálogo de productos | ✔ Incluido |
| Administración del inventario | ✔ Incluido |
| Gestión del carrito de compras | ✔ Incluido |
| Gestión de pedidos | ✔ Incluido |
| Gestión de facturación | ✔ Incluido |
| Gestión de envíos | ✔ Incluido |
| Gestión de devoluciones | ✔ Incluido |
| Gestión de reembolsos | ✔ Incluido |
| Consulta de reportes administrativos | ✔ Incluido |

### **3.2 Procesos Fuera del Alcance**

El presente documento no contempla aspectos relacionados con interfaces gráficas, aplicaciones móviles, portales web, mecanismos de autenticación técnica, tecnologías de implementación, arquitectura del software ni detalles relacionados con el almacenamiento de la información.

## **4\. Descripción General del Marketplace**

La organización facilita la comercialización de productos de terceros mediante una plataforma empresarial unificada, proporcionando la infraestructura necesaria para la operación comercial centralizada.

### **4.1 Componentes Principales del Negocio**

| Componente | Descripción |
| :---- | :---- |
| Usuarios | Personas autorizadas para interactuar con el sistema. |
| Vendedores | Responsables de comercializar productos. |
| Compradores | Usuarios que realizan compras. |
| Bodegas | Lugares donde se administra el inventario físico. |
| Productos | Bienes físicos o digitales ofrecidos. |
| Inventario | Existencias disponibles para comercialización. |
| Pedidos | Solicitudes de compra realizadas por compradores. |
| Facturación | Información comercial asociada a las ventas. |
| Envíos | Procesos logísticos para productos físicos. |

## **5\. Participantes del Negocio**

Cada participante desempeña un único rol dentro del sistema y únicamente podrá interactuar con la información correspondiente a sus funciones.

| Participante | Descripción General |
| :---- | :---- |
| Comprador | Persona que adquiere productos publicados. |
| Vendedor | Responsable de registrar y administrar sus productos. |
| Operador Logístico | Encargado de la operación física de bodegas y despachos. |
| Administrador | Responsable de la administración de vendedores y bodegas. |
| Supervisor | Perfil de consulta y seguimiento operativo. |

## **6\. Modelo Operativo del Marketplace**

El ciclo de negocio sigue un flujo funcional estructurado que garantiza la coherencia operativa.

### **6.1 Flujo General del Negocio**

1. **Incorporación:** El Administrador registra al vendedor y su primera bodega.  
2. **Catálogo:** El vendedor registra productos y define sus características.  
3. **Inventario:** Se registran existencias iniciales en las bodegas asociadas.  
4. **Publicación:** Los productos se hacen visibles en el catálogo público.  
5. **Compra:** El comprador selecciona productos mediante el carrito y confirma el pedido.  
6. **Transacción:** Se valida el pago y se inicia el flujo de preparación.  
7. **Logística:** Se realiza el empaque, despacho y transporte del pedido.  
8. **Cierre:** El pedido se marca como finalizado tras la entrega confirmada.

---

# **DOMINIOS FUNCIONALES DEL NEGOCIO**

## **DOMINIO 1\. Administración de Usuarios**

Este dominio constituye la base de autenticación e identificación del Marketplace.

* **Objetivo:** Garantizar la correcta identificación y estado operativo de los usuarios.  
* **Participantes:** Todos los roles (Comprador, Vendedor, Operador, Administrador, Supervisor).

### **Tabla de Atributos: Usuarios**

| Atributo | Descripción Funcional | Obligatorio | Restricción |
| :---- | :---- | :---- | :---- |
| Identificador | Identifica de forma única al usuario. | Sí | Único |
| Nombre completo | Nombre oficial del usuario. | Sí | No vacío |
| Correo electrónico | Medio principal de acceso y comunicación. | Sí | Único |
| Rol | Define las responsabilidades y permisos. | Sí | Único por usuario |
| Estado | Condición operativa (Activo, Bloqueado, etc.). | Sí | Catálogo definido |

## **DOMINIO 2\. Gestión de Compradores**

* **Objetivo:** Administrar la información específica para la participación en procesos comerciales.  
* **Restricción Clave:** El comprador nunca administrará información de otros compradores ni inventarios.

| Atributo | Descripción | Obligatorio |
| :---- | :---- | :---- |
| Dirección principal | Ubicación habitual para entregas. | Sí |
| Direcciones adicionales | Ubicaciones secundarias de entrega. | No |
| Estado comercial | Condición del comprador para realizar compras. | Sí |

## **DOMINIO 3\. Gestión de Vendedores**

* **Objetivo:** Administrar la incorporación y mantenimiento de proveedores de productos.  
* **Regla de Negocio:** Los vendedores no pueden auto-registrarse; son incorporados por el Administrador.

## **DOMINIO 4\. Gestión de Bodegas**

* **Objetivo:** Controlar los espacios físicos de almacenamiento.  
* **Clasificación:** Se distinguen bodegas del Marketplace y bodegas de Vendedores.

---

# **OPERACIÓN COMERCIAL Y LOGÍSTICA**

## **DOMINIO 5\. Gestión del Catálogo**

El catálogo diferencia entre productos físicos (requieren inventario y despacho) y productos digitales (entrega inmediata tras pago).

| Atributo | Descripción | Tipo de Dato |
| :---- | :---- | :---- |
| Tipo de Producto | Físico o Digital. | Selección |
| Variantes | Diferencias de color, talla, modelo, etc. | Lista |
| Estado | Publicado, Suspendido o Descontinuado. | Selección |

## **DOMINIO 6\. Gestión del Inventario**

El inventario es distribuido y debe estar vinculado obligatoriamente a un producto y una bodega específica.

* **Movimientos:** Ingreso, Reserva, Salida por venta, Ajuste y Devolución.  
* **Restricción:** No se permitirán existencias negativas bajo ninguna circunstancia.

## **DOMINIO 7\. Gestión de Pedidos**

Representa el compromiso comercial formal. Su ciclo de vida es el proceso central del sistema.

### **Ciclo de Estados del Pedido**

1. **Carrito:** Selección provisional de productos.  
2. **Pendiente de Pago:** Espera de confirmación financiera.  
3. **Pagado:** Inicio de procesos de alistamiento.  
4. **Despachado:** Salida física de la bodega.  
5. **Entregado / Finalizado:** Conclusión satisfactoria de la entrega.

---

# **RESTRICCIONES Y CONTROL DE CALIDAD**

## **10\. Restricciones Generales**

| Código | Restricción |
| :---- | :---- |
| RG-01 | Toda operación debe ejecutarse por un usuario autenticado. |
| RG-02 | Cada usuario tendrá un único rol dentro del sistema. |
| RG-03 | Ningún participante podrá administrar información fuera de su rol. |

## **11\. Validaciones Críticas**

* **Inventario:** No se puede reservar inventario inexistente o marcado como "Dañado".  
* **Pedidos:** Un pedido finalizado no podrá ser modificado bajo ninguna circunstancia.  
* **Usuarios:** El documento de identidad y correo electrónico deben ser únicos en la plataforma.

## **12\. Matriz de Responsabilidades**

| Proceso | Comprador | Vendedor | Op. Logístico | Admin |
| :---- | :---- | :---- | :---- | :---- |
| Registro Vendedores |  |  |  | ✔ |
| Registro Productos |  | ✔ |  |  |
| Administración Inventario |  | ✔ | ✔ |  |
| Gestión de Pedidos | ✔ | ✔ | ✔ |  |
| Gestión Reembolsos | ✔ |  |  | ✔ |

