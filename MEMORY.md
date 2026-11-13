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
