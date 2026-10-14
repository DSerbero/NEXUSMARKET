# NEXUSMARKET

## Desarrollo local con Docker

Requisitos: Docker Desktop para Windows con WSL 2 habilitado.

Desde PowerShell, en la carpeta `nexusmarket`:

```powershell
Copy-Item .env.example .env
```

Edita `.env` y reemplaza todos los valores `change_me`. Luego inicia la app y las dos bases:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f app
```

La aplicacion queda en `http://localhost:8080`; MySQL y MongoDB solo se publican en `localhost`, en los puertos `3306` y `27017`. MongoDB crea un usuario de aplicacion separado del administrador. Los volumenes `mysql_data_bootstrap` y `mongo_data_bootstrap` conservan los datos al reiniciar. Los volumenes anteriores se dejan intactos cuando hace falta volver a inicializar cuentas.

### Autenticacion (JWT)

La API usa autenticacion stateless con JWT (HS256). Para obtener un token:

```powershell
curl -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"email\":\"usuario@ejemplo.com\",\"password\":\"secreto\"}'
```

La respuesta es `{ \"accessToken\": \"...\", \"tokenType\": \"Bearer\", \"identifier\": \"...\", \"role\": \"...\" }`. Usa el token en el header `Authorization: Bearer <token>` para el resto de endpoints. Un token invalido, expirado o ausente responde `401`. El secreto de firma y la expiracion se configuran con `APP_SECURITY_JWT_SECRET` y `APP_SECURITY_JWT_EXPIRATION_MS` (ver `application.properties`).

El login valida las credenciales contra el hash en `user_credentials` mediante el Security Adapter; el dominio no genera ni inspecciona el JWT. Todavia no hay endpoint de registro de usuarios de negocio, por lo que para probar el flujo hace falta sembrar un usuario y su credencial en la base.

### API de catalogo inicial

`GET /api/v1/products/{identifier}` consulta un producto publicado y requiere un token Bearer valido. Los productos suspendidos, descontinuados o inexistentes responden `404`.

Todavia no hay endpoint para registrar productos, por lo que un catalogo vacio respondera `404` para cualquier identificador. El alta de productos y el registro de usuarios de negocio quedan para los siguientes slices.

Para detener los contenedores sin borrar los datos: `docker compose down`. `docker compose down -v` tambien elimina los volumenes y borra los datos locales.

La imagen prepara el runtime y la conectividad, pero aun faltan entidades/documentos y adaptadores que implementen los puertos de persistencia del dominio.
