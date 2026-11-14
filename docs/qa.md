# QA automatizada

Suite que valida CIDEM Integrity de punta a punta contra el stack real y, si algo
falla, abre un issue en GitHub con el contexto necesario para reproducirlo.

## Comandos

```bash
./scripts/dev-up.sh          # levanta db + redis + backend + frontend (Docker)
./scripts/smoke.sh           # 14 comprobaciones rápidas (API, SPA, Flyway, reglas SQL)
./scripts/qa.sh              # suite completa + reporte en qa-artifacts/<fecha>/
./scripts/qa.sh --up --triage  # levanta el stack y abre issues con los fallos
./scripts/qa-triage.sh <dir>   # triage de un reporte ya generado
```

En desarrollo sin Docker para el backend:

```bash
./scripts/backend-dev.sh     # Spring Boot en local contra db/redis de Docker
```

## Qué corre `qa.sh`

| Paso | Herramienta | Qué cubre |
|---|---|---|
| backend: tests | JUnit 5 + Testcontainers | 490 tests: dominio, slices MVC, integración con Postgres/Redis |
| frontend: tests | Vitest + jsdom | 61 specs de servicios, componentes y páginas |
| frontend: lint | ESLint + Angular ESLint | reglas de plantillas y TypeScript |
| frontend: build | `ng build` | la app compila en configuración development |
| smoke API | `curl` | health, OpenAPI, auth, endpoints legales protegidos, migraciones y reglas append-only |
| E2E | Playwright (Chromium) | login/registro, guard de rol, centro `/legal`, consentimientos, contrato de API |

Los E2E crean su propio usuario vía API (`e2e/global-setup.ts`), así no dependen
de datos precargados ni del orden de ejecución.

## Artefactos

`qa-artifacts/<AAAA-MM-DD_HHMMSS>/` contiene:

- `reporte.md` / `reporte.json` — tabla de checks (PASS/FAIL/SKIP).
- `logs/` — salida completa de cada paso.
- `junit/backend/` — XML de surefire.
- `playwright/` — traces, vídeos, screenshots y reporte HTML.

La carpeta está en `.gitignore`: son artefactos, no código.

## Triage automático

`qa-triage.sh` lee `reporte.json`, y por cada FAIL:

1. Busca un issue abierto con el mismo título (`[QA] <check> falla`).
2. Si no existe, lo crea con etiqueta `bug`, el commit, la rama y las últimas 40
   líneas del log.
3. Si existe, no duplica.

`--dry-run` muestra qué haría sin crear nada.

En CI lo ejecuta el workflow **Nightly QA**
(`.github/workflows/nightly-qa.yml`) cada noche a las 07:00 UTC, con
`permissions: issues: write` y subida de artefactos.

## Añadir un E2E

1. Crear `integrity-core-fr/e2e/<flujo>.spec.ts`.
2. Usar `authenticatedPage(page)` de `e2e/helpers/api.ts` si hace falta sesión.
3. Ejecutar `npx playwright test` (o `npm run e2e:ui` para depurar).

## Bugs que destapó la primera corrida

Levantar el stack y correr la suite encontró fallos que los tests unitarios no
veían:

- Bean `cacheManager` duplicado (`RedisCacheConfig` + `CacheConfig`): el backend
  no arrancaba con perfil Docker.
- Faltaba la migración de `exam_session`: `ddl-auto: validate` abortaba el
  arranque.
- `JacksonConfig` dependía de `Jackson2ObjectMapperBuilder`, que Spring Boot 4 ya
  no auto-configura.
- `OversightWebSocketConfig` exigía un `AuthenticationManager` inexistente.
- Jackson 3 rechaza `null` en primitivos: los PUT con flags opcionales devolvían
  400.
- `LegalApiService` llamaba a `/legal/...` en vez de `/api/legal/...`.
- `/v3/api-docs` y Swagger estaban bloqueados por seguridad.
- `LegalConfig` rompía los tests `@WebMvcTest` al exigir `AuditService`.
- Healthcheck del frontend usaba `localhost` (IPv6) en un nginx solo IPv4.
- Tests de integración con `Instant` sin tipo SQL, contexto sin datasource y
  `Cache.get()` sin `ValueWrapper`.
- `safeguard-monitor.service.spec.ts` mockeaba `SentinelStompService` sin
  `connectForExam()`.
