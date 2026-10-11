# CIDEM Integrity

Plataforma de integridad academica con un backend Spring Boot y un frontend Angular/Ionic.

## Estructura

- `integrity-core/`: API REST, autenticacion JWT, persistencia JPA, Flyway, evaluator y sentinel.
- `integrity-core-fr/`: aplicacion web Angular/Ionic.
- `docker-compose.yml`: entorno completo con PostgreSQL, Redis, backend y frontend.

## Requisitos

- Java 25
- Node.js y npm
- Docker o Podman con Compose
- PostgreSQL y Redis solo son necesarios si el backend se ejecuta fuera de Compose.

## Ejecutar con Compose

Desde la raiz del repositorio:

```bash
docker compose up -d --build
```

Servicios disponibles:

- Frontend: http://localhost:8081
- Backend: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

Para detener los servicios:

```bash
docker compose down
```

El Compose usa el volumen externo configurado en `docker-compose.yml`. Si el volumen no existe en una instalacion nueva, crealo antes de levantar los servicios:

```bash
docker volume create b98896fb802fe6281d6f268378511036b8ffdb8283b9e5c582ee9fc7f79da7c7
```

## Ejecutar en desarrollo

### Dependencias

```bash
docker compose up -d db redis
```

### Backend

Las variables requeridas son:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/integrity_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export JWT_SECRET=tu_clave_secreta_super_segura_para_jwt_32_caracteres_min
export ADMIN_REGISTRATION_CODE=admin-local
export PROFESOR_REGISTRATION_CODE=profesor-local
```

Luego:

```bash
cd integrity-core
./mvnw spring-boot:run
```

Flyway crea y actualiza el esquema automaticamente al iniciar.

### Frontend

En otra terminal:

```bash
cd integrity-core-fr
npm install
npm start
```

La aplicacion queda disponible en http://localhost:4200 y usa el backend de http://localhost:8080.

## Funcionalidades principales

- Registro e inicio de sesion con JWT.
- Catalogo de paises con Mexico como registro inicial.
- Dashboard por rol: administrador, profesor y alumno.
- Administradores pueden consultar todos los usuarios.
- Profesores pueden consultar los alumnos.
- Alumnos ingresan a examenes mediante codigo de acceso.
- Creacion de examenes con preguntas y opciones correctas en una sola peticion.
- Modulo evaluator para examenes, intentos, respuestas y resultados.
- Modulo sentinel para eventos sospechosos y alertas.

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

Las migraciones de base de datos estan en `integrity-core/src/main/resources/db/migration/` y son administradas por Flyway. No se debe modificar una migracion que ya haya sido aplicada; agrega una nueva con la siguiente version disponible.

## API

La API se publica bajo:

```text
http://localhost:8080
```

El endpoint publico de paises es `GET /auth/countries`. La documentacion OpenAPI, si esta habilitada, esta disponible en `/swagger-ui/index.html`.

## Despliegue del frontend en Vercel

El frontend se despliega como sitio estatico independiente; el backend Spring Boot, PostgreSQL y Redis deben estar desplegados y ser accesibles por HTTPS en otro servicio.

1. Importa este repositorio en Vercel y configura **Root Directory** como `integrity-core-fr`.
2. Conserva los comandos y la salida definidos en `integrity-core-fr/vercel.json`: `npm ci`, build de produccion y directorio `www/`. Las reescrituras SPA permiten que las rutas Angular funcionen al recargar.
3. Configura la variable de entorno `API_URL` en Vercel para Preview y Production con la URL HTTPS publica del backend (sin barra final). El build falla si falta o si no usa HTTPS, en vez de publicar una app apuntando a localhost.
4. En el servicio del backend, configura `CORS_ALLOWED_ORIGINS` con los origenes de Vercel permitidos (dominio de produccion y, si aplica, dominio Preview). No uses `*` con autenticacion por credenciales.

El workflow de GitHub Actions en `.github/workflows/ci.yml` sigue ejecutando tests, lint y builds de backend/frontend en push a `main` y en Pull Requests. No hace un segundo deploy: al conectar el proyecto Vercel con GitHub, Vercel crea Preview Deployments para Pull Requests y Production Deployments desde `main`; usa los checks de GitHub como proteccion de despliegue si se requiere que CI pase antes de publicar. El primer enlace del repositorio y la configuracion de variables se completan en el panel de Vercel; no se guardan tokens ni IDs de proyecto en Git.
