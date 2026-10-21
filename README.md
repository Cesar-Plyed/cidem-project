# CIDEM Integrity

Plataforma de integridad académica con un backend Spring Boot y un frontend Angular/Ionic.

## Estructura

- `integrity-core/`: API REST, autenticación JWT, persistencia JPA, Flyway, evaluator y sentinel.
- `integrity-core-fr/`: aplicación web Angular/Ionic.
- `docker-compose.yml`: entorno completo con PostgreSQL, Redis, backend y frontend.

## Requisitos

- Java 25
- Node.js y npm
- Docker o Podman con Compose
- PostgreSQL y Redis solo son necesarios si el backend se ejecuta fuera de Compose.

## Ejecutar con Compose

Desde la raíz del repositorio:

```bash
docker compose up -d --build
```

Servicios disponibles:

- Frontend: http://localhost:8081
- Backend: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- PostgreSQL: localhost:5432
- Redis: localhost:6379

Para detener los servicios:

```bash
docker compose down
```

## Ejecutar en desarrollo

### Dependencias

```bash
docker compose up -d db redis
```

### Backend

Copia el archivo `.env.example` y define valores reales en tu entorno local antes de ejecutar:

```bash
cp .env.example .env
# Luego edita .env con tus valores locales
```

El backend usa estas variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/integrity_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=change_me_postgres_password
export JWT_SECRET=change_me_jwt_secret_32_chars
export ADMIN_REGISTRATION_CODE=change_me_admin_code
export PROFESOR_REGISTRATION_CODE=change_me_professor_code
```

Luego:

```bash
cd integrity-core
./mvnw spring-boot:run
```

Flyway crea y actualiza el esquema automáticamente al iniciar.

### Frontend

En otra terminal:

```bash
cd integrity-core-fr
npm install
npm start
```

La aplicación queda disponible en http://localhost:4200 y usa el backend de http://localhost:8080.

## Funcionalidades principales

- Registro e inicio de sesión con JWT.
- Catálogo de países con México como registro inicial.
- Dashboard por rol: administrador, profesor y alumno.
- Administradores pueden consultar todos los usuarios.
- Profesores pueden consultar los alumnos.
- Alumnos ingresan a exámenes mediante código de acceso.
- Creación de exámenes con preguntas y opciones correctas en una sola petición.
- Módulo evaluator para exámenes, intentos, respuestas y resultados.
- Módulo sentinel para eventos sospechosos y alertas.

## Pruebas y validaciones

Backend:

```bash
cd integrity-core
./mvnw test
```

Frontend:

```bash
cd integrity-core-fr
npm run build -- --configuration development
npm run lint
```

## Migraciones

Las migraciones de base de datos están en `integrity-core/src/main/resources/db/migration/` y son administradas por Flyway. No se debe modificar una migración que ya haya sido aplicada; agrega una nueva con la siguiente versión disponible.

## API

La API se publica bajo:

```text
http://localhost:8080
```

El endpoint público de países es `GET /auth/countries`. La documentación OpenAPI, si está habilitada, está disponible en `/swagger-ui/index.html`.

## Despliegue del frontend en Vercel

El frontend se despliega como sitio estático independiente; el backend Spring Boot, PostgreSQL y Redis deben estar desplegados y ser accesibles por HTTPS en otro servicio.

1. Importa este repositorio en Vercel y configura **Root Directory** como `integrity-core-fr`.
2. Conserva los comandos y la salida definidos en `integrity-core-fr/vercel.json`: `npm ci`, build de producción y directorio `www/`.
3. Configura la variable de entorno `API_URL` en Vercel para Preview y Production con la URL HTTPS pública del backend (sin barra final).
4. En el servicio del backend, configura `CORS_ALLOWED_ORIGINS` con los orígenes de Vercel permitidos.

## Releases alpha de Tauri

El workflow `.github/workflows/tauri-alpha.yml` valida el backend, ejecuta lint y compila instaladores para Linux, Windows, macOS Apple Silicon y macOS Intel.

Para crear una alpha:

```bash
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

Configura `CORS_ALLOWED_ORIGINS` en el backend para aceptar los orígenes del frontend web y de Tauri.
