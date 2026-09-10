# Módulo de Autenticación — `integrity_core.auth`

Documentación técnica del módulo de autenticación y autorización basado en JWT, con arquitectura hexagonal (puertos y adaptadores) sobre Spring Boot + PostgreSQL.

---

## 1. Arquitectura

El módulo sigue arquitectura hexagonal: el **dominio** no depende de Spring, JPA ni JWT. Toda dependencia externa entra a través de **puertos** (interfaces) implementados en `infrastructure`.

```
auth/
├── domain/
│   ├── model/          Entidades y value objects de negocio (sin anotaciones de framework)
│   ├── repository/     Puertos de persistencia (interfaces)
│   ├── service/         Puertos de seguridad (interfaces)
│   └── exception/       Excepciones de negocio
│
├── application/
│   ├── usecase/          Casos de uso (orquestan dominio + puertos)
│   └── dto/              Comandos de entrada a los casos de uso
│
└── infrastructure/
    ├── persistence/
    │   ├── entity/       Entidades JPA (@Entity)
    │   ├── repository/   Interfaces Spring Data JPA
    │   ├── mapper/        Dominio ↔ Entidad JPA
    │   └── adapter/        Implementan los puertos domain/repository
    ├── security/          PasswordHasher, TokenProvider, filtro JWT, SecurityFilterChain
    └── web/
        ├── controller/     REST controllers
        ├── dto/            Request/Response de la API
        └── exception/       @RestControllerAdvice
```

**Regla de dependencia:** `domain` no importa nada de `infrastructure`. `application` solo conoce `domain`. `infrastructure` es la única capa que conoce Spring, JPA y JWT.

---

## 2. Modelo de dominio

| Clase | Tipo | Descripción |
|---|---|---|
| `User` | Entidad | Nombre, email, hash de password, país, teléfonos, **rol** (`Role`). Valida invariantes en el constructor. |
| `Country` | Value object (`record`) | Catálogo de países. Valida nombre e ISO code. |
| `Phone` | Entidad | Número de teléfono de un usuario, con flag `isPrimary`. |
| `Role` | Enum | `ADMINISTRATIVO`, `PROFESOR`, `ALUMNO`. Un usuario tiene **exactamente un** rol. |
| `RefreshToken` | Entidad | Token de refresco con `expiresAt` y `revoked`. Expone `isValid()`, `isExpired()`, `revoke()`. |
| `AuthTokens` | DTO (`record`) | Par `accessToken` / `refreshToken` que devuelven login y refresh. |

---

## 3. Sistema de roles

Un usuario tiene **un único rol**: `ADMINISTRATIVO`, `PROFESOR` o `ALUMNO`.

### Reglas de asignación de rol al registrarse

| Rol | Cómo se obtiene |
|---|---|
| `ALUMNO` | Registro público libre, sin restricciones (`POST /auth/register`). |
| `PROFESOR` | Registro público (`POST /auth/register`) + campo `registrationCode` que debe coincidir con `security.registration-codes.profesor`. |
| `ADMINISTRATIVO` | Registro público (`POST /auth/register`) + campo `registrationCode` que debe coincidir con `security.registration-codes.administrativo`, **o** vía `POST /auth/admin/register` por un usuario ya autenticado con rol `ADMINISTRATIVO` (sin necesidad de código). |

La comparación de códigos usa `MessageDigest.isEqual` (tiempo constante) para no filtrar información por timing attack.

### Autorización en endpoints

El rol viaja como claim `role` dentro del JWT de acceso. El filtro `JwtAuthenticationFilter` lo traduce a una `GrantedAuthority` con prefijo `ROLE_` (ej. `ROLE_PROFESOR`), lo que permite usar:

```java
@PreAuthorize("hasRole('ADMINISTRATIVO')")
@PreAuthorize("hasAnyRole('ADMINISTRATIVO', 'PROFESOR')")
```

en cualquier controller del proyecto, no solo en el módulo `auth`.

---

## 4. Flujo de autenticación

```
1. POST /auth/register           → crea el usuario, no autentica
2. POST /auth/login              → valida credenciales, devuelve { accessToken, refreshToken }
3. Cliente guarda ambos tokens
4. Cliente llama endpoints protegidos con: Authorization: Bearer <accessToken>
5. Cuando el accessToken expira (401) → POST /auth/refresh con el refreshToken
   → el refresh token usado se REVOCA y se emite un par nuevo (rotación)
6. POST /auth/logout con el refreshToken → lo revoca (cierra sesión en ese dispositivo)
```

### Rotación de refresh tokens

Cada vez que se usa un refresh token en `/auth/refresh`, ese token se marca `revoked = true` y se emite uno nuevo. Si alguien intenta reusar un refresh token ya usado, `RefreshToken.isValid()` devuelve `false` y la petición se rechaza — esto permite detectar robo de tokens (reuso indebido).

---

## 5. Endpoints

Base path: `/auth`

| Método | Ruta | Auth requerida | Rol | Body | Descripción |
|---|---|---|---|---|---|
| POST | `/auth/register` | No | — | `RegisterRequest` | Registro público. `role=ALUMNO` libre; `PROFESOR`/`ADMINISTRATIVO` requieren `registrationCode`. |
| POST | `/auth/login` | No | — | `LoginRequest` | Devuelve `AuthResponse` (access + refresh token). |
| POST | `/auth/refresh` | No* | — | `RefreshTokenRequest` | Rota el refresh token, devuelve `AuthResponse` nuevo. *Se autentica con el refresh token en el body, no con Bearer. |
| POST | `/auth/logout` | No* | — | `RefreshTokenRequest` | Revoca el refresh token dado. |
| POST | `/auth/admin/register` | **Sí** (Bearer) | `ADMINISTRATIVO` | `RegisterRequest` | Crea un usuario con cualquier rol, sin código de registro. |

### `RegisterRequest`

```json
{
  "firstNames": "Juan Carlos",
  "lastNames": "Perez Lopez",
  "email": "juan.perez@example.com",
  "password": "unaContraseñaSegura123",
  "countryId": "b3f1c2a0-....-....-....-............",
  "phoneNumber": "+52 449 123 4567",
  "role": "ALUMNO",
  "registrationCode": null
}
```

`registrationCode` se omite o va en `null` si `role = ALUMNO`.

### `LoginRequest`

```json
{ "email": "juan.perez@example.com", "password": "unaContraseñaSegura123" }
```

### `AuthResponse`

```json
{ "accessToken": "eyJhbGciOi...", "refreshToken": "3fa85f64-5717-..." }
```

### `RefreshTokenRequest`

```json
{ "refreshToken": "3fa85f64-5717-..." }
```

### `UserResponse` (respuesta de registro)

```json
{
  "id": "a1b2c3d4-....",
  "firstNames": "Juan Carlos",
  "lastNames": "Perez Lopez",
  "email": "juan.perez@example.com",
  "countryName": "México",
  "role": "ALUMNO"
}
```

**Nunca** se expone `passwordHash` en ninguna respuesta.

### Errores

Todo error sigue el formato `ErrorResponse`:

```json
{
  "timestamp": "2026-09-10T18:22:31.123-06:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Correo o contrasena invalidos",
  "fieldErrors": null
}
```

| Excepción de dominio | HTTP |
|---|---|
| `EmailAlreadyRegisteredException` | 409 Conflict |
| `InvalidCredentialsException` | 401 Unauthorized |
| `InvalidRefreshTokenException` | 401 Unauthorized |
| `InvalidAccessTokenException` | 401 Unauthorized |
| `InvalidRoleAssignmentException` (código de registro inválido/faltante) | 403 Forbidden |
| `AccessDeniedException` (rol insuficiente en `@PreAuthorize`) | 403 Forbidden |
| `CountryNotFoundException` / `UserNotFoundException` | 404 Not Found |
| `IllegalArgumentException` (invariantes de dominio, ej. país sin nombre) | 400 Bad Request |
| `MethodArgumentNotValidException` (Bean Validation en el DTO) | 400 Bad Request (incluye `fieldErrors`) |
| Cualquier otra excepción no controlada | 500 Internal Server Error (mensaje genérico, sin stack trace) |

---

## 6. Seguridad

### Password hashing
BCrypt vía `PasswordEncoder` de Spring Security, detrás del puerto `PasswordHasher`.

### JWT
- Firmado con HMAC-SHA (`io.jsonwebtoken` / JJWT).
- Claims: `sub` (userId), `email`, `role`, `iat`, `exp`.
- Expiración configurable, default 15 minutos.
- El secreto (`security.jwt.secret`) debe tener mínimo 32 caracteres y **nunca** debe hardcodearse ni subirse a git.

### Refresh tokens
- Valor aleatorio (`UUID.randomUUID()`), no es un JWT.
- Persistido en la tabla `refresh_tokens` con expiración (default 30 días) y flag `revoked`.
- Rotación obligatoria en cada uso (ver sección 4).

### SecurityFilterChain
- API completamente **stateless** (sin sesiones de servidor, sin cookies).
- CSRF deshabilitado (no aplica sin autenticación basada en cookies).
- Rutas públicas: `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`.
- Cualquier otra ruta requiere `Authorization: Bearer <accessToken>` válido.
- `@EnableMethodSecurity` habilita `@PreAuthorize` en cualquier controller de la aplicación.

### Registration codes
- Uno por rol elevado (`administrativo`, `profesor`), leídos de configuración/variables de entorno.
- **Limitación conocida:** son códigos fijos y compartidos (no de un solo uso ni expirables). Si se necesita mayor control, migrar a una tabla `registration_codes` con estado `used`/`expires_at`. *(Pendiente, no implementado en esta iteración.)*

---

## 7. Configuración requerida

### `pom.xml` — dependencias

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

*(Opcional pero recomendado: `flyway-core` + `org.postgresql:postgresql` para versionar las migraciones SQL provistas en `src/main/resources/db/migration`.)*

### `application.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/integrity_core
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate   # el esquema lo maneja Flyway/SQL manual, no Hibernate
    open-in-view: false
  flyway:
    enabled: true

security:
  jwt:
    secret: ${JWT_SECRET}                          # minimo 32 caracteres
    access-token-expiration-minutes: 15
    refresh-token-expiration-days: 30
  registration-codes:
    administrativo: ${ADMIN_REGISTRATION_CODE}
    profesor: ${PROFESOR_REGISTRATION_CODE}
```

**Ninguno de estos valores debe tener un default hardcodeado en el repo.** Usar variables de entorno (`.env` local + secretos del entorno en producción).

### Base de datos

Orden de migraciones:
1. Tu script SQL original (`countries`, `users`, `phone_numbers`, `refresh_tokens`) → `V1__init_schema.sql`.
2. `V2__add_role_to_users.sql` (incluido) → agrega `users.role`.

---

## 8. Ejemplo de flujo completo (cURL)

```bash
# 1. Registro de alumno (sin código)
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstNames": "Ana", "lastNames": "Ruiz", "email": "ana@example.com",
    "password": "contraseñaSegura1", "countryId": "<uuid-pais>",
    "phoneNumber": "4491234567", "role": "ALUMNO"
  }'

# 2. Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "ana@example.com", "password": "contraseñaSegura1" }'
# → { "accessToken": "...", "refreshToken": "..." }

# 3. Llamada autenticada a cualquier endpoint protegido
curl http://localhost:8080/algun-recurso \
  -H "Authorization: Bearer <accessToken>"

# 4. Refrescar cuando expira el access token
curl -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "<refreshToken>" }'

# 5. Logout
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "<refreshToken>" }'
```

---

## 9. Pendientes / mejoras futuras (no implementadas)

Estas quedaron identificadas durante el desarrollo pero fuera del alcance actual:

- **Registration codes de un solo uso / expirables**: hoy son fijos por config; migrar a tabla en BD si se necesita mayor control.
- **`Phone.equals()`/`hashCode()`**: no implementados; puede causar comportamiento inesperado al comparar/deduplicar teléfonos en listas.
- **Validación de teléfono duplicado**: `User.addPhone` no valida que el número ya exista en la lista.
- **Endpoint de "logout en todos los dispositivos"**: el caso de uso `LogoutUseCase.executeForAllDevices(userId)` ya existe pero no está expuesto por ningún controller todavía.
- **Tests**: no se generaron pruebas unitarias/de integración en esta iteración.
- **Rate limiting** en `/auth/login` y `/auth/register` para mitigar fuerza bruta — no implementado.
- **CORS**: no configurado; agregar según el/los dominios del frontend cuando se conozcan.

---

## 10. Inventario de archivos del módulo

```
domain/model/          AuthTokens, Country, Phone, RefreshToken, Role, User
domain/repository/     UserRepository, CountryRepository, RefreshTokenRepository
domain/service/        PasswordHasher, TokenProvider, RegistrationCodeValidator
domain/exception/      DomainException, EmailAlreadyRegisteredException,
                        InvalidCredentialsException, InvalidRefreshTokenException,
                        InvalidAccessTokenException, InvalidRoleAssignmentException,
                        CountryNotFoundException, UserNotFoundException

application/dto/        RegisterUserCommand, LoginCommand
application/usecase/    RegisterUserUseCase, LoginUseCase,
                        RefreshAccessTokenUseCase, LogoutUseCase

infrastructure/persistence/entity/      CountryEntity, UserEntity, PhoneEntity, RefreshTokenEntity
infrastructure/persistence/repository/  UserJpaRepository, CountryJpaRepository, RefreshTokenJpaRepository
infrastructure/persistence/mapper/      CountryMapper, PhoneMapper, UserMapper, RefreshTokenMapper
infrastructure/persistence/adapter/     UserRepositoryImpl, CountryRepositoryImpl, RefreshTokenRepositoryImpl

infrastructure/security/    SecurityBeansConfig, BCryptPasswordHasher, JwtTokenProvider,
                             ConfigRegistrationCodeValidator, JwtAuthenticationFilter, SecurityConfig

infrastructure/web/controller/   AuthController
infrastructure/web/dto/          RegisterRequest, LoginRequest, RefreshTokenRequest,
                                  AuthResponse, UserResponse, ErrorResponse
infrastructure/web/exception/    GlobalExceptionHandler

resources/db/migration/    V2__add_role_to_users.sql
resources/                 application-security-snippet.yml
```