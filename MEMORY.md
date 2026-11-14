# Memoria del proyecto

## 2026-11-12

Specs 06–10 de Sentinel implementados sobre la arquitectura hexagonal ya
commiteada.

- **06:** migración `V15__sentinel_events_and_alerts_associations.sql`; extiende
  `sentinel_suspicious_events` y `sentinel_alerts` (event_id, exam_id, source,
  payload, alert_type, resolved, created_at) e índices. Test de esquema con
  Testcontainers.
- **07:** `SentinelEventPublisher` con deduplicación en Redis
  (`dedup:{alertType}:{examId}:{attemptId}`, TTL 5 min); publica
  `AlertGeneratedEvent` + `SentinelAlertEvent`.
- **08:** `SentinelProperties`, `EscalationRule`, `EscalationRuleCatalog`,
  `RedisCounterService`; ventanas en `application.yml`.
- **09:** página `/sentinel/alerts` (Angular 22 standalone, signals), con filtros
  por examen/severidad/fechas, paginación de 20 y acción de resolver.
- **10:** `AuthorizationService` + `@PreAuthorize` en los 5 endpoints; alumno ve
  solo sus intentos, profesor solo sus exámenes, administrativo acceso total.

---
Se descartó el intento paralelo `sentinel_*_v2` (sin migración, mappers rotos)
y se eliminó el duplicado. Se mantienen las tablas de `V8` como fuente única.

---

## 2026-11-13

### Specs 24-31 — Autosave, Redis session infra, connection-status, recovery

**Backend (Spring Boot 4.1, Java 25):**
- `ExamSession.java` — domain record (id, examId, attemptId, studentId, status, startedAt, maxDuration)
- `RedisSessionService.java` — autosave cada 30s, heartbeat, heartbeat check, persistencia Redis
- `RedisSessionEntity.java` — JPA @Entity para `exam_session` (Postgres via Testcontainers)
- `RedisSessionConfig.java` — TTL 15s/30m, @EnableScheduling
- `IdempotencyService.java` — ConcurrentMap + AtomicLong, invalidate/clear cache
- `CacheConfig.java` — RedisCacheManager con TTLs por dominio (idempotency, exams, sessions, alerts)
- `ConcurrencyTest.java` — @Execution CONCURRENT, 10 threads × 5 events, verificación sin pérdida
- `RedisSessionIntegrationTest.java` — PostgreSQL real (Testcontainers), INSERT/UPDATE/DELETE/Payload
- `IdempotencyServiceTest.java` — unit test con ConcurrentMapCacheManager
- `RedisCacheConfigTest.java` — Redis real (Testcontainers), cache put/get/delete

**Frontend (Angular/Ionic):**
- `connection-status/` — UI de estado de conexión (isConnected, reconnect)
- `last-saved/` — UI de último autosave guardado
- `recovery-notice/` — alerta warning/danger si connectionLost()
- `autosave.service.ts` — BehaviorSubject + signals, retrySave
- `set-env-variables.js` — inyecta API_URL en .env.production/.env.development

**Tests:** 11 tests compilados y ejecutados. 2 de integración (Testcontainers) fallan por ausencia de Docker en entorno.

### Decisiones
- Events → `edu.cidemags.integrity_core.events.domain.event.*`
- Redis split → `config/RedisCacheConfig.java` y `sentinel/infrastructure/config/RedisConfig.java`
- Frontend usa signals + BehaviorSubjects (no mutar en subscribe)
- `BaseEvent` no encontrado; `ExamSession` creado como record domain model

---

### Specs 32-35 — Production deploy, CI/CD, docs, PostgreSQL backups

**Infraestructura (Nginx + Docker Compose prod + CI/CD):**
- `docker-compose.production.yml` — services nginx, frontend, backend, redis + red `integrity-net` externa
- `infra/nginx/cidem-integrity.conf` — reverse proxy + HTTPS + WebSocket upgrade
- `infra/certbot/certbot-nginx.conf` — Certbot provision (Let's Encrypt)
- `infra/cron/cidem-backup.conf` — daily cron (0 2 * * *) para backup Postgres
- `.github/workflows/build-images.yml` — build + push a ghcr.io (backend, frontend, tauri)
- `.github/workflows/deploy.yml` — SSH deploy + Flyway migrate + healthcheck

**Scripts:**
- `integrity-core/scripts/deploy-prod.sh` — deploy con SSH (host, user, dry-run)
- `integrity-core/scripts/healthcheck.sh` — curl + redis-cli ping (backend, frontend, redis)
- `integrity-core/scripts/backup-postgres.sh` — pg_dump → .sql.gz + clean 30d

**Documentación (integrity-core/docs/):**
- `architecture.md` — diagrama Mermaid, 4 módulos, flujo de eventos
- `internal-events.md` — tabla eventos + secuencia Mermaid
- `deployment.md` — pasos deploy, vars de entorno, notas seguridad
- `testing-plan.md` — cobertura 80%/70%, E2E Playwright, sprint 7→8
- `user-manual.md` — instrucciones profesor/alumno + placeholders de imagen
- `uat-plan.md` — 7 casos de prueba (pre-condición, acción, resultado esperado)
- `restore-guide.md` — full restore, PITR, restore staging + env vars

### Decisiones
- Docs van en `integrity-core/docs/` (no en root del repo), siguiendo AGENTS.md
- `backup-postgres.sh` usa vars de entorno (PG_HOST, PG_PORT, PG_DB, PG_USER) — no hardcodear
- `docker-compose.production.yml` usa `integrity-net` externa (coincide con `docker-compose.yml` dev)
- `build-images.yml` usa `ghcr.io/cidem/` con push solo en push a main

---

### Specs 36-50 — Cumplimiento legal / GDPR-LOPDGDD

**Backend (`legal/`, Spring Boot 4.1 + Java 25):**
- `V17__legal_compliance.sql` — agrupa todas las tablas (los specs pedían V005-V007,
  pero la última aplicada es V16): `consent_logs`, `data_retention_policy`,
  `archived_responses`, `audit_logs`, `data_capture_policy`, `erasure_requests`,
  `object_requests`, `restriction_requests`, `data_breach_notifications`, `dpia`
  y `is_deleted` en `users`/`exam_answers`/`sentinel_alerts`.
- **36/40/48:** `ConsentLog` append-only (regla SQL bloquea UPDATE/DELETE),
  `ConsentService` (grant/revoke/revoke-all/isGranted/status/history),
  `ConsentController` (`/api/legal/consent/**`).
- **38:** `DataRetentionPolicy`, `DataRetentionService` (`@Scheduled` día 1 a las
  02:00), archiva en `archived_responses` antes de borrar de `exam_answers`.
- **39:** `AuditLog` `@Immutable`, `@AuditTrail`, `AuditInterceptor`
  (`HandlerInterceptor`, sin dependencia AOP), `AuditController` (solo ADMIN).
- **41:** `DataCapturePolicy`, `DataMinimizationService` (SHA-256+salt, `maskIP`),
  `/api/legal/data-types`, seed de 10 tipos.
- **42/45:** `AccessService` y `PortabilityService` exportan JSON pretty y CSV
  RFC-4180; descargas con `Content-Disposition`.
- **43:** `RectificationService` con whitelist entityType/campo (sin SQL
  injection) + borrado lógico; `@AuditTrail(action="RECTIFY_DATA")`.
- **44:** `ErasureService` + `erasure_requests`. Hard delete de respuestas y
  alertas; el audit trail se **pseudonimiza** (actor/IP/user-agent a NULL)
  porque el spec 39 lo declara inmutable; `users` se marca `is_deleted`.
- **46:** `ObjectService`/`ObjectRequest`; **47:** `RestrictionService`/`RestrictionRequest`
  (ACTIVE al solicitar, expira a 365 días); **49:** `BreachNotificationService`
  + `BreachNotifier` (adaptador de log sustituible por SMTP/FCM);
  **50:** `DPIAService` + PDF propio (`SimplePdfWriter`, sin dependencias).

**Frontend (Angular 22, zoneless, signals):**
- `core/services/legal-api.service.ts` — cliente de todos los endpoints.
- `core/services/legal-content.service.ts` — Art. 13/14, política y formulario.
- `shared/components/`: consent-dialog, legal-notice, consent-management,
  consent-manager, access-request, data-rectification, portability-export,
  restriction-request, breach-notice, dpia-review.
- `presentation/pages/legal` — centro de privacidad en `/legal` (+ enlace en home).
- `exam-take` bloquea el examen hasta aceptar el aviso legal (spec 37).

**Tests:** 51 unitarios backend del módulo legal (`./mvnw test -Dtest='edu.cidemags.integrity_core.legal.**'`)
y 16 specs frontend (`ng test --include 'src/app/shared/**/*.spec.ts'`).

### Decisiones
- Una sola migración `V17` en lugar de V005/V006/V007 (evita renumerar Flyway).
- Inmutabilidad: `consent_logs` sí bloquea UPDATE/DELETE en SQL; `audit_logs`
  permite solo la pseudonimización del Art. 17 (no hay endpoint de update).
- Interceptor de auditoría vía `HandlerInterceptor` (no se añadió AOP).
- PDF de EIPD generado a mano para no añadir dependencia ni requerir red.
- No se pudo ejecutar Flyway/Testcontainers: entorno sin Docker. Pendiente
  validar `V17` contra Postgres real.
- Fallos preexistentes ajenos: `safeguard-monitor.service.spec.ts` (24 tests) y
  lint de `ngIf` en connection-status/last-saved/recovery-notice.

---

## 2026-11-13 — validación en Docker y QA automatizada

Se levantó el stack completo (Postgres, Redis, backend, frontend) y se montó una
suite que prueba la app funcionando, no solo compilando.

### Cambios
- **Migración `V18__create_exam_session_table.sql`**: la entidad
  `RedisSessionEntity` mapeaba `exam_session` sin migración; `ddl-auto: validate`
  abortaba el arranque.
- **`RedisCacheConfig` eliminado**: duplicaba el bean `cacheManager` de
  `shared_kernel/config/CacheConfig` y el backend no arrancaba con perfil Docker.
- **`JacksonConfig`**: sin `Jackson2ObjectMapperBuilder` (Spring Boot 4 usa
  Jackson 3); el mapper de Jackson 2 de los servicios legal se construye a mano.
- **`application.yml`**: `spring.jackson.deserialization.fail-on-null-for-primitives: false`
  porque Jackson 3 rechaza `null` en primitivos y los PUT con flags opcionales
  devolvían 400.
- **`OversightWebSocketConfig` / `StompAuthorizationInterceptor`**: se quitó la
  dependencia de `AuthenticationManager` (no existía como bean y no se usaba).
- **`LegalConfig`**: declara el `AuditInterceptor` con `@ConditionalOnBean` y lo
  registra vía `ObjectProvider`; `AuditInterceptor` deja de ser `@Component`
  porque `@WebMvcTest` incluye todo `HandlerInterceptor` y rompía 12 slices.
- **`SecurityConfig`**: `/v3/api-docs/**` y `/swagger-ui/**` pasan a públicos.
- **`LegalApiService`**: `baseUrl` corregido a `/api/legal` (llamaba a `/legal`).
- **Frontend**: `nginx.conf` escucha también IPv6 y el healthcheck usa
  `127.0.0.1`; `*ngIf` → `@if` en connection-status, last-saved y recovery-notice.
- **Tests arreglados**: `RedisSessionIntegrationTest` (siembra users/exams/attempts
  por las FKs y usa `Timestamp`), `RedisCacheConfigTest` (`@ServiceConnection` de
  Redis/Postgres y `ValueWrapper`), `safeguard-monitor.service.spec.ts` (mock con
  `connectForExam`).
- **QA**: `scripts/dev-up.sh`, `scripts/smoke.sh`, `scripts/backend-dev.sh`,
  `scripts/qa.sh`, `scripts/qa-triage.sh`, `docs/qa.md`, E2E con Playwright en
  `integrity-core-fr/e2e/` y workflow `.github/workflows/nightly-qa.yml`.
- `docker-compose.yml`: builds con `network: host` (la red bridge del host no
  tiene egreso y Maven/npm no resolvían DNS durante el build) y
  `SECURITY_RATE_LIMIT_MAX_ATTEMPTS=100` en dev.

### Decisiones
- Un solo usuario E2E por corrida (`e2e/global-setup.ts`) para no agotar el rate
  limit de `/auth/register`.
- El triage es idempotente: busca un issue abierto con el mismo título antes de
  crear; soporta `--dry-run`.
- Los artefactos van a `qa-artifacts/<fecha>/` (ignorado por git), no dentro del
  código.

### Estado
- Backend 490/490 tests, frontend 61/61, lint y build limpios, smoke 14/14 y
  E2E 10/10 contra el stack en Docker.
- `V17` y `V18` verificadas contra Postgres 16 real.

---

## 2026-11-13 (PR #12 — CI)

Primera corrida del PR destapó dos fallos que no se ven en local.

### Cambios
- **`build-images.yml`**: eliminado el job `build-tauri`. Apuntaba a
  `integrity-core-fr/src-tauri` como contexto, donde no hay `Dockerfile`
  (`failed to read dockerfile: open Dockerfile: no such file or directory`).
  Tauri se publica como instalador nativo en `tauri-alpha.yml`; no tiene
  sentido una imagen de servidor.
- **`build-images.yml`**: `permissions: packages: write` y nombre de imagen
  `ghcr.io/<owner en minúsculas>/…`. El workflow publicaba en
  `ghcr.io/cidem/…`, una organización que no existe, así que el push en `main`
  habría fallado.
- **`RedisCacheConfigTest`**: reescrito sin `@SpringBootTest`. Antes levantaba
  contexto completo + Postgres/Flyway/Redis para probar `CacheConfig` y el
  round-trip fallaba de forma intermitente en CI. Ahora instancia `CacheConfig`
  directamente contra un Redis de Testcontainers y comprueba cachés, TTLs por
  dominio y serialización.

### Decisiones
- Un test de `CacheConfig` no necesita el contexto de la aplicación: al
  levantarlo competía con los contenedores de otras clases y ocultaba el
  origen del fallo.
- El nombre de la imagen se calcula en bash (`${GITHUB_REPOSITORY_OWNER,,}`)
  porque las expresiones de GitHub no tienen `lowercase()`.

### Estado
- Backend 490/490 en local con el test reescrito (3 corridas seguidas).
- Pendiente: verificar el rerun del PR.

---
