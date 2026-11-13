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
