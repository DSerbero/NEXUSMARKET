# Propuesta de mejora de NexusMarket (basada en el proyecto bancario de referencia)

> Documento de análisis. NO es código de producción: son ejemplos del "antes → después"
> para que decidas qué fases aplicar. Fuente de patrones:
> `construccion_de_software_2_2026_2` (proyecto bancario que tu SDD cita como referencia).

---

## Resumen de hallazgos

| Aspecto | Banco (referencia) | NexusMarket (actual) |
|---|---|---|
| SRP de servicios | 1 servicio = 1 operación | 1 servicio = 1 agregado con varios métodos |
| Autorización | Servicios colaboradores separados y reutilizables | Reglas inline (`private require*`) |
| Errores de dominio | Excepciones específicas | 1 sola `DomainValidationException` |
| Password/JWT | Puertos de dominio (`PasswordServicePort`, `JwtServicePort`) | Implementaciones directas en infra |
| Validación REST | Bean Validation + `@RestControllerAdvice` | Sin validación de beans ni advice |

---

## FASE 1 — Excepciones de dominio específicas + mapeo HTTP

### Antes (actual): todo es DomainValidationException, REST no distingue 404/401/400
```java
// app/domain/exceptions/DomainValidationException.java
public class DomainValidationException extends RuntimeException {
    public DomainValidationException(String message) { super(message); }
}
```
```java
// ProductController: cualquier error de dominio -> 404
@ExceptionHandler(DomainValidationException.class)
public ResponseEntity<Void> handleUnavailableProduct() {
    return ResponseEntity.notFound().build();   // "no publicado" y "no existe" colapsan a 404
}
```

### Después: jerarquía de excepciones + un solo advice global
```java
// app/domain/exceptions/DomainException.java
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) { super(message); }
}

// app/domain/exceptions/EntityNotFoundException.java        -> HTTP 404
public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entity) { super(entity + " not found"); }
}

// app/domain/exceptions/UnauthorizedOperationException.java -> HTTP 403
public class UnauthorizedOperationException extends DomainException {
    public UnauthorizedOperationException(String message) { super(message); }
}

// app/domain/exceptions/InvalidCredentialsException.java    -> HTTP 401
public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() { super("Invalid credentials or inactive user"); }
}

// app/domain/exceptions/ValidationException.java            -> HTTP 400
public class ValidationException extends DomainException {
    public ValidationException(String message) { super(message); }
}
```
```java
// app/adapters/rest/exception/RestExceptionHandler.java  (NUEVO)
@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)        -> 404
    @ExceptionHandler(InvalidCredentialsException.class)    -> 401
    @ExceptionHandler(UnauthorizedOperationException.class) -> 403
    @ExceptionHandler(ValidationException.class)            -> 400
}
```
**Impacto:** bajo. Es aditivo. `DomainValidationException` puede quedarse como alias de
`ValidationException` para no romper los 41 tests actuales de golpe (migración gradual).
**Beneficio:** la API responde códigos HTTP semánticos; los controllers dejan de tener
`@ExceptionHandler` sueltos.

---

## FASE 2 — Servicios de autorización reutilizables

### Antes (actual): reglas inline duplicadas en cada servicio
```java
// ProductService.java
private void requireSeller(Seller seller) {
    if (!UserRole.SELLER.equals(seller.getRole())) {
        throw new DomainValidationException("Only sellers can manage products");
    }
}
private void requireOwner(Seller seller, Product product) {
    if (!DomainValidations.sameIdentity(seller, product.getSeller())) {
        throw new DomainValidationException("Seller does not own product");
    }
}
// ...y UserService, BuyerService, SellerService repiten validateActive/requireAdmin a mano.
```

### Después: colaboradores de autorización inyectados (patrón del banco)
```java
// app/domain/services/authorization/ValidateActiveUserService.java
@Service
public class ValidateActiveUserService {
    public void execute(User user) {
        if (user == null) throw new ValidationException("user is required");
        if (!UserStatus.ACTIVE.equals(user.getStatus()))
            throw new UnauthorizedOperationException("User must be active");
    }
}

// app/domain/services/authorization/ValidateRoleService.java
@Service
public class ValidateRoleService {
    public void execute(User user, UserRole required) {
        if (!required.equals(user.getRole()))
            throw new UnauthorizedOperationException("Role " + required.getCode() + " required");
    }
}

// app/domain/services/authorization/ValidateOwnershipService.java
@Service
public class ValidateOwnershipService {
    public void execute(User user, User owner) {
        if (!DomainValidations.sameIdentity(user, owner))
            throw new UnauthorizedOperationException("User does not own the resource");
    }
}
```
```java
// ProductService.java (después) — colaboradores inyectados, sin private require*
@Service
public class ProductService {
    private final ProductRepositoryPort productRepository;
    private final ValidateActiveUserService validateActiveUser;
    private final ValidateRoleService validateRole;
    private final ValidateOwnershipService validateOwnership;

    public Product register(Seller seller, Product product) {
        validateActiveUser.execute(seller);
        validateRole.execute(seller, UserRole.SELLER);
        ...
    }
}
```
**Impacto:** medio. Toca las firmas de constructor de los servicios (y sus tests). Alto beneficio:
autorización consistente y testeable una sola vez.

---

## FASE 3 — Password/JWT como puertos de dominio

### Antes (actual, de la seguridad que hicimos): el controller llama a JwtService de infra
```java
// AuthController (infra) depende de JwtService (infra) para emitir token
String token = jwtService.generateToken(user);
```

### Después: el dominio depende de interfaces; BCrypt/JJWT quedan como adaptadores
```java
// app/domain/ports/out/PasswordServicePort.java  (dominio)
public interface PasswordServicePort {
    String encrypt(String rawPassword);
    boolean matches(String rawPassword, String hashed);
}

// app/domain/ports/out/JwtServicePort.java  (dominio)
public interface JwtServicePort {
    String generateToken(User user);
    Optional<Claims> parseToken(String token);   // o un TokenClaims del dominio
}
```
```java
// app/domain/services/user/LoginService.java  (implementa LoginUseCase, patrón banco)
@Service
public class LoginService implements LoginUseCase {
    private final UserRepositoryPort userRepository;
    private final PasswordServicePort passwordService;
    private final JwtServicePort jwtService;
    private final ValidateActiveUserService validateActiveUser;

    @Override
    public AuthenticationResult login(String email, String password) {
        User stored = userRepository.findByEmail(emailQuery(email))
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordService.matches(password, stored.getPasswordHash()))
            throw new InvalidCredentialsException();
        validateActiveUser.execute(stored);
        return new AuthenticationResult(stored, jwtService.generateToken(stored));
    }
}
```
```java
// app/infrastructure/security/JjwtServiceAdapter.java implements JwtServicePort   (mi JwtService actual)
// app/infrastructure/security/BcryptPasswordServiceAdapter.java implements PasswordServicePort
```
**Impacto:** medio. Reorganiza la seguridad que ya funciona, pero la acerca al estándar del SDD
("el dominio no debe conocer JWT/frameworks de seguridad" — anti-patrón §23.2).
**Beneficio:** `LoginService` es 100% testeable sin BCrypt/JJWT reales (mocks de puertos).

---

## FASE 4 — SRP más granular (selectivo, NO masivo)

El banco parte TODO en 1-servicio-1-método. En NexusMarket eso sería un refactor enorme y de bajo
retorno. Solo dividiría el servicio más gordo:

### Antes (actual): ProductService con 8 métodos
```java
public class ProductService {
    register / consult / consultPublished / update / publish / suspend / discontinue
}
```

### Después: separar registro/actualización del ciclo de vida de estado
```java
// app/domain/services/product/ProductCatalogService.java   -> register, consult, update
// app/domain/services/product/ProductLifecycleService.java -> publish, suspend, discontinue, consultPublished
```
**Impacto:** medio-alto (mueve métodos y ajusta el `PublicAccessUseCaseImpl` + tests).
**Beneficio:** cada clase con una razón clara para cambiar.

---

## FASE 5 — Validación de entrada REST

### Antes: DTOs sin validación
```java
public record LoginRequest(String email, String password) {}
```

### Después: Bean Validation + advice con envelope
```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password) {}
```
```java
// @RestControllerAdvice con MethodArgumentNotValidException -> 400 + body estándar
```
**Impacto:** bajo. Aditivo.

---

## Mi recomendación

1. **Fase 1** (excepciones) — hazlo sí o sí. Es la base para que las demás tengan sentido.
2. **Fase 2** (autorización) — alto valor, es lo que más se repite hoy.
3. **Fase 3** (puertos de seguridad) — alinea lo que ya construimos con el SDD.
4. Fase 4 y 5 — opcionales / para cuando haya tiempo.

Todas son graduables: puedo aplicar 1 sola fase, o varias, manteniendo los 41 tests en verde en cada paso.
