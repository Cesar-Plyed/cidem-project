# CIDEM Integrity - Project Flow Documentation

## Overview

This document describes the complete flow of the CIDEM Academic Integrity Platform, covering the backend (Spring Boot 4.1, Java 25) and frontend (Angular 22 + Ionic 9) with Tauri v2 for desktop demo capabilities.

> Every endpoint, class and table named below was verified against the source tree on 2026-10-09.
> Items that are designed but **not implemented yet** are marked `⚠️ not implemented` and are
> tracked in `ToDo.md` § *Pendientes de auditoría*.

## Architecture

### Backend Modules (Hexagonal/Ports-Adapters)

```
integrity-core/
├── auth/                 # Authentication & Authorization (JWT, roles, registration)
│   ├── application/  domain/  infrastructure/
├── evaluator/            # Exam management, attempts, answers, reviews
│   ├── application/  domain/  infrastructure/
├── sentinel/             # Suspicious events + alerts, STOMP /ws/sentinel
│   ├── ports/  adapters(in|out)/  application/  domain/
├── safeguard/            # Exam restrictions config, monitored sessions, block events
│   ├── application/  domain/  infrastructure/
├── oversight/            # Teacher dashboard data (summary, timeline, integrity index)
│   ├── ports/  adapters(in|out)/  application/  domain/
├── shared_kernel/        # Shared exceptions, utilities (placeholder, README only)
├── shared/events/        # Cross-module event contracts (SafeguardBlockEvent today)
└── config/               # Redis, CORS, security configuration
```

Module state (do not assume parity):

| Module | Persistence | Web surface |
|--------|-------------|-------------|
| `auth` | Postgres (`V1`,`V2`,`V5`) | `/auth/*` REST |
| `evaluator` | Postgres (`V3`,`V4`,`V6`) | `/api/evaluator/*` REST |
| `safeguard` | Postgres (`V7`) | `/api/safeguard/*` REST |
| `sentinel` | Postgres (`V8`) + Redis window | STOMP `/ws/sentinel` only (no REST) |
| `oversight` | Postgres (`V9`) | `/api/oversight/*` REST (polling) |

### System context

```mermaid
flowchart LR
    subgraph Client["integrity-core-fr (Angular 22 + Ionic 9)"]
        PAGES["12 lazy pages<br/>login · register · home · exam-list · exam-editor<br/>exam-results · exam-reviews · exam-result · exam-attempt<br/>exam-take · oversight · evaluator-demo"]
        GUARDS["roleGuard(ADMINISTRATIVO / PROFESOR / ALUMNO)"]
        IG["AuthInterceptor + RefreshInterceptor<br/>(token in localStorage)"]
        SG["SafeGuardMonitorService<br/>tab/blur/copy/shortcut watchdog"]
        PAGES --> GUARDS --> IG
        PAGES --> SG
    end

    subgraph Tauri["Tauri v2 (desktop demo)"]
        NATIVE["get_system_info · show_notification<br/>block_keyboard_shortcut · get_active_window_title<br/>take_screenshot"]
    end

    subgraph Server["integrity-core (Spring Boot 4.1 · modular monolith)"]
        AUTH["auth<br/>/auth/*"]
        EVAL["evaluator<br/>/api/evaluator/*"]
        SAFE["safeguard<br/>/api/safeguard/*"]
        SENT["sentinel<br/>/ws/sentinel (STOMP)"]
        OVER["oversight<br/>/api/oversight/*"]
        BUS["Spring ApplicationEventPublisher<br/>+ @EventListener (in-process)"]
        SHARED["shared.events<br/>SafeguardBlockEvent"]
    end

    subgraph Data["Datastores"]
        PG[("PostgreSQL 16<br/>Flyway V1..V9")]
        REDIS[("Redis 7<br/>sentinel event window")]
    end

    PAGES -->|REST + Bearer JWT| AUTH & EVAL & SAFE & OVER
    SG -->|REST events| SAFE
    PAGES -.->|"⚠️ no WS client yet"| SENT
    NATIVE -.-> SG

    EVAL & SAFE --> BUS --> SHARED
    SHARED --> OVER
    BUS --> OVER

    AUTH & EVAL & SAFE & OVER --> PG
    SENT --> PG
    SENT --> REDIS
```

### Container / ports

| Process | Port | Notes |
|---------|------|-------|
| backend | 8080 | Flyway migrates on startup, `ddl-auto: validate` |
| frontend | 8081 | Dockerfile + `www/` output (Vercel/`vercel.json` rewrites `environment.prod.ts` from `API_URL`) |
| Postgres | 5432 | external compose volume `b98896fb802f…` must be created first |
| Redis | 6379 | sentinel rolling window |

---

## Core Flows

### 1. Authentication Flow

Actual controllers: `AuthController` (`@RequestMapping("/auth")`) — **no `/api` prefix**.

| Method | Path | Notes |
|--------|------|-------|
| GET | `/auth/countries` | public, seeded by `V5` |
| POST | `/auth/register` | public, needs role registration code |
| POST | `/auth/admin/register` | `@PreAuthorize` ADMINISTRATIVO |
| POST | `/auth/admin/registration-codes` | ADMINISTRATIVO |
| POST | `/auth/login` | → access + refresh token |
| POST | `/auth/refresh` | rotates the refresh token (rate limited) |
| POST | `/auth/logout`, `/auth/logout-all` | revokes refresh tokens |
| GET | `/auth/users` | ADMINISTRATIVO |

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant FE as Angular (login/register)
    participant API as AuthController
    participant UC as RegisterUserUseCase / LoginUseCase
    participant DB as PostgreSQL

    U->>FE: correo + contraseña (+ código de rol)
    FE->>API: POST /auth/register (o /auth/login)
    Note over API: @Valid → 400 con fieldErrors si algo falta
    API->>UC: execute(command)
    UC->>DB: BCrypt hash / verifica credenciales
    alt login OK
        UC->>DB: INSERT refresh_tokens (UUID, expira 7 días)
        UC-->>API: AuthTokens(accessToken 15 min, refreshToken 7 d)
        API-->>FE: 200 + tokens
        FE->>FE: SessionService guarda el token en localStorage
    else credenciales inválidas
        UC-->>API: InvalidCredentialsException
        API-->>FE: 401 "Correo o contraseña invalidos"
    end
```

**Roles**: `ADMINISTRATIVO`, `PROFESOR`, `ALUMNO` (`roleGuard(...)` in `app.routes.ts`, plus `@PreAuthorize` on staff endpoints).

---

### 2. Exam Creation Flow (Professor)

```mermaid
sequenceDiagram
    autonumber
    actor P as Profesor
    participant FE as exam-editor / exam-list
    participant EX as ExamController
    participant UC as CreateExamUseCase / ExamManagementUseCase
    participant DB as PostgreSQL

    P->>FE: "Nuevo examen" (título, descripción, preguntas)
    FE->>EX: POST /api/evaluator/exams (@Valid)
    EX->>EX: createdByUserId = JWT principal (el del cliente se ignora)
    EX->>UC: execute(new CreateExamCommand(...))
    UC->>DB: INSERT exams (status DRAFT) + questions/options
    UC-->>FE: 201 Exam
    P->>FE: editar mientras DRAFT (PUT /exams/{id}, POST /exams/{id}/questions)
    P->>FE: publicar
    FE->>EX: POST /api/evaluator/exams/{id}/publish
    EX->>EX: assertCanManage (ADMINISTRATIVO o dueño)
    UC->>DB: status → PUBLISHED (requiere ≥1 pregunta)
    P->>FE: generar código de acceso
    FE->>EX: POST /api/evaluator/exams/{id}/access-code?expiresInMinutes=20
    UC->>DB: INSERT exam_access_codes (4–50 caracteres)
    Note over FE: los GET de examen son staff-only:<br/>serializan correctAnswerText / isCorrect
```

Main endpoints: `GET/POST /api/evaluator/exams`, `GET/PUT/DELETE /api/evaluator/exams/{examId}`,
`publish`, `close`, `questions`, `question-banks/{bankId}/import`, `access-code`,
`results`, `results/page`, `results/export` (CSV), `average`, and
`/api/evaluator/question-banks` CRUD.

---

### 3. Student Exam Taking Flow

```mermaid
sequenceDiagram
    autonumber
    actor S as Alumno
    participant FE as exam-attempt
    participant EV as ExamAttemptController
    participant SG as SafeguardController
    participant BUS as ApplicationEventPublisher
    participant OV as Oversight

    S->>FE: código de acceso
    FE->>EV: POST /api/evaluator/attempts/join (o /join-by-code)
    EV->>EV: studentId = JWT principal
    EV-->>FE: ExamAttempt (IN_PROGRESS)
    FE->>SG: POST /api/safeguard/sessions/start
    SG-->>FE: sessionId + config + riskScore=0
    loop durante el examen
        FE->>FE: SafeGuardMonitor vigila tab/blur/copy/atajos
        alt violación detectada
            FE->>SG: POST /api/safeguard/events
            SG->>SG: SafeguardRiskPolicy → puntos de riesgo
            SG->>BUS: publica RiskEventRecorded → SafeguardBlockEvent
            BUS->>OV: SafeguardBlockEventListener → log + índice
        end
        FE->>SG: POST /api/safeguard/sessions/{id}/heartbeat (cada heartbeatIntervalSeconds)
    end
    S->>FE: responder / autoguardado de respuestas
    FE->>EV: POST /api/evaluator/attempts/{attemptId}/answers (questionId obligatorio)
    EV->>EV: ownership: el intento debe ser del estudiante
    S->>FE: entregar
    FE->>EV: POST /api/evaluator/attempts/{attemptId}/submit
    EV->>EV: ownership + examen abierto
    EV->>BUS: ExamSessionFinishedEvent
    FE->>SG: POST /api/safeguard/sessions/{id}/close
    FE->>EV: GET /api/evaluator/attempts/{attemptId}/result
```

**SafeGuard Protection** (active during exam): tab switch (`visibilitychange`), window blur,
copy/paste/cut, context menu, restricted shortcuts (F12, Ctrl+Shift+I…). Every violation is a
`POST /api/safeguard/events` call; risk points are computed by `SafeguardRiskPolicy` and the
session moves `ACTIVE → FLAGGED → TERMINATED` once `riskThreshold` / `autoTerminateOnThreshold`
apply.

---

### 4. SafeGuard Monitoring Flow

```mermaid
flowchart TD
    subgraph Profesor
        CFG["PUT /api/safeguard/exams/{examId}/config<br/>enabled · requireCamera · requireFullscreen<br/>blockCopyPaste · maxTabSwitches · riskThreshold · autoTerminateOnThreshold"]
    end
    subgraph Alumno["Alumno (SafeGuardMonitorService)"]
        START["POST /api/safeguard/sessions/start"]
        HB["POST /sessions/{id}/heartbeat"]
        EV["POST /api/safeguard/events"]
        CLOSE["POST /sessions/{id}/close"]
    end
    subgraph Backend
        POL["SafeguardRiskPolicy<br/>puntos por tipo de evento"]
        LOCK["findByIdForUpdate (PESSIMISTIC_WRITE)<br/>+ saveAndFlush"]
        THRESH{"riskScore ≥ threshold?"}
        EVT["RiskEventRecorded"]
        BLK["shared.events.SafeguardBlockEvent"]
    end

    CFG --> START
    START --> LOCK
    HB --> POL --> LOCK
    EV --> POL --> LOCK
    THRESH -->|"sí"| FLAG["status FLAGGED / TERMINATED"]
    THRESH -->|"no"| OK["status ACTIVE"]
    LOCK --> EVT --> BLK --> OV["Oversight listener<br/>log + índice de integridad"]
    CLOSE --> LOCK
```

Rules (approved 2026-10-01): config write requires `@PreAuthorize("hasAnyRole('ADMINISTRATIVO','PROFESOR')")`;
students may **read** config only for exams where they have an attempt.

---

### 5. Sentinel Event Processing Flow

Sentinel has **no REST controller**: the only ingress is the STOMP endpoint `/ws/sentinel`
(JWT interceptor builds `SentinelPrincipal`), destination `/app/sentinel/events`.

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente (⚠️ sin cliente WS hoy)
    participant WS as SentinelWebSocketController
    participant UC as ReceiveSuspiciousEventService
    participant PG as PostgreSQL (V8)
    participant R as Redis (window)
    participant Q as /queue/sentinel/alerts

    C->>WS: SEND /app/sentinel/events {examSessionId, studentId, attemptId, type, occurredAt}
    Note over WS: ⚠️ los ids vienen del payload,<br/>no se contrastan con el principal del JWT
    WS->>UC: receive(command)
    UC->>R: ZADD evento (ventana 5 min, dedup por attempt+type)
    alt duplicado
        UC-->>WS: Optional.empty()
    else nuevo
        UC->>PG: INSERT sentinel_suspicious_events
        UC->>UC: cuenta eventos en ventana (escalación)
        alt umbral de alerta alcanzado
            UC->>PG: INSERT sentinel_alerts
            UC-->>WS: Optional<Alert>
            WS->>Q: convertAndSendToUser(principal, AlertNotification)
        else
            UC-->>WS: Optional.empty()
        end
    end
```

Ports: `SuspiciousEventRepositoryPort` → `JpaSuspiciousEventRepositoryAdapter`,
`AlertRepositoryPort` → `JpaAlertRepositoryAdapter`, `EventWindowPort` → `RedisEventWindowAdapter`.

---

### 6. Oversight Monitoring Flow (Teacher Dashboard)

Oversight today is **REST + polling**; the real-time layer is designed but not implemented.

```mermaid
sequenceDiagram
    autonumber
    actor T as Profesor
    participant FE as oversight.page (3 pestañas)
    participant API as OversightController
    participant L as @EventListener (in-process)
    participant BUS as ApplicationEventPublisher
    participant DB as PostgreSQL (V9)

    T->>FE: /exams/:examId/oversight
    par carga inicial
        FE->>API: GET /api/oversight/exams/{examId}/summary
        FE->>API: GET /api/oversight/exams/{examId}/timeline
    end
    API-->>FE: 200 JSON
    loop cada 30 s (interval + switchMap)
        FE->>API: summary + timeline otra vez
    end
    T->>FE: clic en fila / botón del alumno
    FE->>API: GET /api/oversight/attempts/{attemptId}/integrity
    API-->>FE: IntegritySnapshot (tarjeta inline)

    Note over BUS,L: eventos en el mismo proceso
    SAFE->>BUS: SafeguardBlockEvent
    SENT->>BUS: SentinelAlertEvent (interno)
    EVAL->>BUS: EvaluatorStateEvent / ExamSessionStarted(Finished)Event
    BUS->>L: listeners de oversight → INSERT log + actualiza snapshot
```

**Integrity index** (`IntegrityScoringPolicy`): base 100, penalizaciones por evento; snapshot
guardado con lectura bloqueante (`findByAttemptIdForUpdate`, `PESSIMISTIC_WRITE`) para evitar
lost updates entre el polling y los listeners.
Risk levels: `GREEN ≥ 80`, `YELLOW 50–79`, `RED < 50`.

> ⚠️ **Not implemented**: `/ws/oversight` STOMP endpoint and `OversightWebSocketService`
> (described in the old plan) do not exist — neither does any WebSocket client in
> `integrity-core-fr` (no `@stomp/stompjs` dependency). Today the UI is fed by polling only.

---

### 7. Redis Usage (Evaluator autosave ⚠️ not implemented)

What Redis actually does today:

- **Sentinel event window** (`RedisEventWindowAdapter`, sorted set, 5 min TTL) for deduplication
  and escalation counting.

What the original Sprint 6 design described but is **not in the codebase**:

- `evaluator:session:{attemptId}` autosave endpoints
  (`PUT /api/evaluator/sessions/{attemptId}/answers|time|question|activity|submit`) and
  `GET /api/evaluator/sessions/{attemptId}` recovery — no `ExamSession` class, controller or
  Redis adapter exists under `evaluator/`. Tracked in `ToDo.md`.

---

### 8. Event Contracts (Shared Events)

All events share standard fields:

```json
{
  "eventId": "uuid",
  "examId": "uuid",
  "attemptId": "uuid",
  "examSessionId": "uuid",
  "studentId": "uuid",
  "occurredAt": "ISO8601",
  "source": "MODULE_NAME",
  "payload": { }
}
```

| Event | Published by | Consumed by | Contract |
|-------|--------------|-------------|----------|
| `SafeguardBlockEvent` | `safeguard` bridge | `oversight` listener (registered), `evaluator` listener (empty, Sprint 7) | **`shared.events`** ✅ |
| `RiskEventRecorded` | `safeguard` (internal) | safeguard bridge → `SafeguardBlockEvent` | internal |
| `SentinelAlertEvent` | `sentinel` (intended) | `oversight` listener | ⚠️ naming not unified |
| `EvaluatorStateEvent` | `evaluator` (answer submit) | `oversight` listener | internal |
| `ExamSessionStartedEvent` / `ExamSessionFinishedEvent` | `evaluator` (join / submit) | `oversight` listener | internal |

```mermaid
flowchart LR
    subgraph publish["Publicadores"]
        SAFE["safeguard<br/>SafeguardBlockEventBridge"]
        SENT["sentinel"]
        EVAL["evaluator<br/>join / answer / submit"]
    end
    BUS["Spring ApplicationEventPublisher<br/>(in-process, misma JVM)"]
    subgraph consume["Consumidores"]
        OV1["oversight<br/>SafeguardBlockEventListener"]
        OV2["oversight<br/>AlertGeneratedEventListener"]
        OV3["oversight<br/>EvaluatorStateEventListener"]
        EV1["evaluator<br/>SafeguardBlockEventListener (vacío)"]
    end
    SAFE --> BUS
    SENT --> BUS
    EVAL --> BUS
    BUS --> OV1 & OV2 & OV3 & EV1
```

⚠️ Design debt: listeners run **inside** the publisher's transaction (should be
`@TransactionalEventListener(AFTER_COMMIT)`), and only `SafeguardBlockEvent` uses the shared
contract package. See `ToDo.md` § *Pendientes de auditoría*.

---

### 9. Tauri Desktop Demo Capabilities

**Native Commands (SafeGuard integration):**
- `get_system_info()` — OS, arch, hostname
- `show_notification(title, body)` — desktop notifications
- `block_keyboard_shortcut(shortcut)` — OS-level shortcut blocking
- `get_active_window_title()` — active window monitoring
- `take_screenshot()` — screen capture for proctoring

**Security CSP:**

```
default-src 'self' ipc: http://localhost:8080 ws://localhost:8080
script-src 'self' 'unsafe-inline' 'unsafe-eval'
connect-src 'self' http://localhost:8080 ws://localhost:8080
```

---

## End-to-end sequence (examen completo)

```mermaid
sequenceDiagram
    autonumber
    actor P as Profesor
    actor S as Alumno
    participant FR as Angular
    participant BE as Spring Boot
    participant DB as PostgreSQL
    participant R as Redis

    P->>FR: crea y publica examen
    FR->>BE: POST /api/evaluator/exams (+ publish + access-code)
    BE->>DB: exams · questions · exam_access_codes

    S->>FR: entra con el código
    FR->>BE: POST /api/evaluator/attempts/join
    BE->>DB: exam_attempts (IN_PROGRESS)
    FR->>BE: POST /api/safeguard/sessions/start
    BE->>DB: safeguard_session

    loop durante el intento
        alt restricción violada
            FR->>BE: POST /api/safeguard/events
            BE->>DB: safeguard_event
            BE->>BE: SafeguardRiskPolicy + SafeguardBlockEvent
            BE->>DB: oversight_log_entries · oversight_integrity_snapshots
        end
        FR->>BE: POST /attempts/{id}/answers
        BE->>DB: exam_answers · exam_answer_selected_options
        FR->>BE: heartbeat SafeGuard
    end

    S->>FR: entregar
    FR->>BE: POST /api/evaluator/attempts/{id}/submit
    BE->>DB: exam_attempts (SUBMITTED)
    FR->>BE: POST /api/safeguard/sessions/{id}/close

    P->>FR: resultados, revisiones y oversight
    FR->>BE: GET /api/evaluator/exams/{id}/results · /reviews/open-ended · /api/oversight/…
    BE->>DB: joins + índice de integridad
```

---

## Data Model

```mermaid
erDiagram
    countries ||--o{ users : belongs
    users ||--o{ phone_numbers : has
    users ||--o{ refresh_tokens : has
    users ||--o{ registration_codes : issues
    users ||--o{ exams : creates
    exams ||--o{ questions : has
    questions ||--o{ question_options : has
    exams ||--o{ exam_access_codes : issues
    exams ||--o{ exam_attempts : receives
    users ||--o{ exam_attempts : attempts
    exam_attempts ||--o{ exam_answers : answers
    exam_answers }o--|| questions : about
    exam_answers ||--o{ exam_answer_selected_options : marks
    exam_answers ||--o| evaluation_reviews : reviewed_by
    users ||--o{ question_banks : creates
    question_banks ||--o{ questions : stores

    exams ||--o| safeguard_configuration : configured
    exam_attempts ||--o{ safeguard_session : monitored_by
    safeguard_session ||--o{ safeguard_event : records
    exam_attempts ||--o{ sentinel_suspicious_events : raises
    sentinel_suspicious_events ||--o{ sentinel_alerts : escalates_to
    exam_attempts ||--o{ oversight_log_entries : logs
    exam_attempts ||--o|| oversight_integrity_snapshots : scores
```

Migrations: `V1` auth core · `V2` registration codes · `V3` evaluator schema ·
`V4` question bank owner · `V5` seed México · `V6` review timestamps ·
`V7` safeguard tables · `V8` sentinel tables · `V9` oversight tables.
Never edit an applied `V*`; add the next version.

---

## Data Flow Summary

```mermaid
flowchart LR
    STU["Alumno (frontend)"] -->|"join · answers · submit"| EVAL["evaluator"]
    STU -->|"violaciones · heartbeat"| SG["safeguard"]
    PROF["Profesor (frontend)"] -->|"exams · results · reviews"| EVAL
    PROF -->|"polling 30 s"| OV["oversight"]
    PROF -->|"config"| SG

    EVAL --> PG[("PostgreSQL")]
    SG --> PG
    SG -->|"SafeguardBlockEvent"| OV
    SENT["sentinel"] --> PG
    SENT -->|"ventana 5 min"| R[("Redis")]
    OV --> PG
    OV -->|"REST"| PROF
```

---

## Deployment

### Docker Compose (Local/Production)

```yaml
services:
  db:       PostgreSQL 16   # volumen externo: crearlo antes con `docker volume create …`
  redis:    Redis 7
  backend:  Spring Boot (8080)
  frontend: Angular/Ionic (8081)
```

### Environment Variables (Backend)

- `JWT_SECRET` (≥32 chars) — no default, the app fails to start without it
- `ADMIN_REGISTRATION_CODE`, `PROFESOR_REGISTRATION_CODE`
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` (outside Compose)
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:8081,http://localhost:4200`)
- `security.rate-limit.trust-proxy-header` (default `false`: `X-Forwarded-For` ignored)

### CI/CD Pipeline (`.github/workflows/ci.yml`, push/PR → `main`)

1. Backend: `./mvnw -B --no-transfer-progress verify` → `docker build`
2. Frontend: `npm ci` → `npm run lint` → production build → `docker build` (order matters)
3. Deploy: Oracle Cloud / VPS with Nginx reverse proxy
4. Healthcheck: `/actuator/health`

---

## Key Technologies

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 4.1, Java 25, Maven |
| Database | PostgreSQL 16 (Flyway migrations, `ddl-auto: validate`) |
| Cache/Windows | Redis 7 (sentinel event window) |
| Auth | JWT (access 15 min, refresh 7 d) |
| Real-time | STOMP over WebSocket (`/ws/sentinel`) |
| Frontend | Angular 22 (zoneless), Ionic 9, Bootstrap 5, TypeScript |
| Desktop | Tauri v2, Rust |
| Testing | JUnit 5, Testcontainers, Vitest |
| Build | Maven, npm, Docker |

---

## Testing Strategy

- **Unit tests**: domain logic, policies, mappers (no containers)
- **Integration tests**: `@SpringBootTest` + Testcontainers (PostgreSQL 16 + Redis) — needs a
  container runtime (Docker/Podman); tests do not need the app env vars
- **Web layer**: `@WebMvcTest` + `@Import(SecurityConfig.class)` for authorization slices
  (`AuthControllerTest`, `SafeguardConfigAuthorizationTest`, `AdminRegisterAuthorizationTest`,
  `EvaluatorRequestValidationTest`)
- **Frontend**: Vitest (`safeguard-monitor.service.spec.ts`); CI runs **lint + production build
  only** — don't re-enable the test step until more specs exist
- **All 352 backend tests passing**

---

## Known Limitations (MVP)

1. No full OS-level lockdown (Tauri demo only shows capabilities)
2. No mobile apps (Android/iOS)
3. No advanced reporting/analytics
4. No Grafana/Prometheus monitoring
5. No RabbitMQ/event bus (uses Spring `ApplicationEventPublisher`)
6. No high availability/microservices (modular monolith)
7. SafeGuard `metadata` field accepts JSON only (500 on invalid input)
8. Four event types without policy rules: `DEVTOOLS_OPENED`, `CAMERA_BLOCKED`,
   `MICROPHONE_ANOMALY`, `SCREEN_SHARE_STOPPED`
9. No WebSocket on the frontend: oversight polls every 30 s; `/ws/oversight` doesn't exist
10. Refresh tokens are stored in plain text and both tokens live in `localStorage`
11. `WebSocketConfig` allows `setAllowedOriginPatterns("*")`
12. `/exams/:id/take` is a dead end (`submitAttempt` always throws); the real route is
    `/attempts/:attemptId`

Full audit backlog: `ToDo.md` § *Pendientes de auditoría (Fase 2)*.

---

## Sprint Progress

| Sprint | Status | Deliverable |
|--------|--------|-------------|
| 1 | ✅ | Base integration, shared events, contracts |
| 2 | ✅ | Sentinel persistence (V8) + deduplication (Redis window) |
| 3 | ✅ | Oversight MVP (backend + frontend panel) |
| 4 | ✅ | STOMP endpoint `/ws/sentinel` (oversight WS ⚠️ pending) |
| 5 | ✅ | SafeGuard backend + frontend + Tauri demo |
| 6 | 🔄 | Redis window done; evaluator autosave sessions ⚠️ pending |
| 7 | 🔄 | Full integration test flow |
| 8 | ⏳ | Release candidate, deploy, docs |

---

## File Locations

### Backend Key Files
- `integrity-core/src/main/java/.../shared/events/*.java` — event contracts
- `integrity-core/src/main/java/.../sentinel/adapters/in/websocket/` — `/ws/sentinel`
- `integrity-core/src/main/java/.../oversight/adapters/in/event/` — event listeners → log/snapshot
- `integrity-core/src/main/java/.../auth/infrastructure/web/exception/GlobalExceptionHandler.java` — error mapping (400/401/403/404/409/500)
- `integrity-core/src/main/resources/db/migration/V*.sql` — Flyway migrations

### Frontend Key Files
- `integrity-core-fr/src/app/app.routes.ts` — routes + `roleGuard`
- `integrity-core-fr/src/app/presentation/pages/oversight/` — oversight dashboard
- `integrity-core-fr/src/app/application/safeguard/safeguard-monitor.service.ts` — SafeGuard monitor
- `integrity-core-fr/src/app/presentation/pages/exam-attempt/` — student exam with SafeGuard
- `integrity-core-fr/DESIGN-SYSTEM.md` — UI guide (Ocean Depths + Bootstrap 5)
- `integrity-core-fr/src-tauri/src/lib.rs` — Tauri native commands

---

*Verified against source: 2026-10-09*
