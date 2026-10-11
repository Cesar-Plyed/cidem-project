# CIDEM Integrity — Grapify del Proyecto

> Vista gráfica (Mermaid) del proyecto completo: contexto, arquitectura, módulos,
> eventos, flujos, datos y stack. Fuente verificada: `FLOW.md` + código (2026-10-10).

---

## 1. Resumen en una imagen

| Dimensión | Valor |
|---|---|
| Tipo | Modular monolith + SPA (plataforma de integridad académica / proctoring) |
| Backend | `integrity-core` — Spring Boot 4.1, Java 25, hexagonal (262 clases, 61 tests, 352 tests verdes) |
| Frontend | `integrity-core-fr` — Angular 22 zoneless + Ionic 9 + Bootstrap 5 (93 TS, 15 HTML, 11 páginas, Tauri v2) |
| Datos | PostgreSQL 16 (Flyway V1..V10) + Redis 7 (ventana de eventos Sentinel) |
| Despliegue | Docker Compose (8080 backend / 8081 frontend / 5432 Pg / 6379 Redis) + CI GitHub Actions + Vercel |
| Progreso | Sprints 1-5 ✅ · 6-7 🔄 · 8 ⏳ |

```mermaid
mindmap
  root((CIDEM Integrity))
    Backend integrity-core
      auth JWT roles registros
      evaluator exámenes intentos resultados
      safeguard configuración sesiones riesgo
      sentinel eventos STOMP /ws/sentinel
      oversight bitácora índice integridad
      shared.events SafeguardBlockEvent
    Frontend integrity-core-fr
      11 páginas Angular Ionic
      roleGuard ADMIN PROFESOR ALUMNO
      SafeGuardMonitorService
      Tauri v2 demo escritorio
    Datos
      PostgreSQL V1..V10
      Redis ventana 5 min
    Infra
      Docker Compose
      GitHub Actions CI
      Vercel frontend
```

---

## 2. Contexto del sistema

```mermaid
flowchart LR
    A["👤 Administrativo"] --> UI
    P["👨‍🏫 Profesor"] --> UI
    S["🧑‍🎓 Alumno"] --> UI

    subgraph UI["Frontend — Angular 22 + Ionic 9 (Tauri v2 opcional)"]
        PAGES["11 lazy pages"]
        MON["SafeGuardMonitorService<br/>tab / blur / copy / atajos"]
        GUARDS["roleGuard + AuthInterceptor"]
    end

    subgraph BE["Backend — Spring Boot 4.1 modular monolith"]
        AUTH["auth"]
        EVAL["evaluator"]
        SAFE["safeguard"]
        SENT["sentinel"]
        OVER["oversight"]
        BUS[("ApplicationEventPublisher<br/>in-process")]
    end

    PG[("PostgreSQL 16<br/>Flyway V1..V10")]
    R[("Redis 7<br/>ventana eventos")]

    UI -->|"REST + Bearer JWT"| BE
    MON -.->|"⚠️ sin cliente WS aún"| SENT

    AUTH & EVAL & SAFE & OVER --> PG
    SENT --> PG
    SENT --> R
    EVAL & SAFE & SENT --> BUS
    BUS --> OVER
```

---

## 3. Módulos del backend y su superficie

```mermaid
flowchart TD
    subgraph M["edu.cidemags.integrity_core"]
        AUTH["📦 auth<br/>JWT · roles · registro<br/>REST /auth/*"]
        EVAL["📦 evaluator<br/>exámenes · intentos · respuestas<br/>resultados · reviews<br/>REST /api/evaluator/*"]
        SAFE["📦 safeguard<br/>config · sesiones · riesgo<br/>REST /api/safeguard/*"]
        SENT["📦 sentinel<br/>eventos sospechosos · alertas<br/>STOMP /ws/sentinel (sin REST)"]
        OVER["📦 oversight<br/>bitácora · índice integridad<br/>REST /api/oversight/* (polling)"]
        SH["📦 shared.events<br/>SafeguardBlockEvent"]
        SK["📦 shared_kernel<br/>placeholder"]
    end

    AUTH -.->|"alimenta JWT principal"| EVAL & SAFE & SENT & OVER
    SAFE -->|"RiskEventRecorded → bridge"| SH
    SH -->|"stub (Sprint 7)"| EVAL
    SENT -->|"⚠️ AlertGeneratedEvent (fuga de módulo)"| OVER
```

| Módulo | Persistencia | Superficie web | Estado |
|---|---|---|---|
| auth | Pg (V1,V2,V5) | REST `/auth/*` | ✅ completo |
| evaluator | Pg (V3,V4,V6,V10) | REST `/api/evaluator/*` | ✅ completo, sin eventos propios |
| safeguard | Pg (V7) | REST `/api/safeguard/*` | ✅ MVP (4 tipos sin regla de riesgo) |
| sentinel | Pg (V8) + Redis | STOMP `/ws/sentinel` | ✅ backend, sin cliente WS |
| oversight | Pg (V9) | REST `/api/oversight/*` | ✅ MVP, polling 30 s |
| shared_kernel | — | — | placeholder |

---

## 4. Grafo de eventos internos (lo que existe hoy)

```mermaid
flowchart LR
    subgraph PUB["Publicadores"]
        SG["safeguard<br/>SafeguardService"]
        BRI["safeguard bridge<br/>RiskEventRecorded → BlockEvent"]
        SEN["sentinel<br/>ReceiveSuspiciousEventService"]
    end

    BUS[("Spring ApplicationEventPublisher<br/>(misma JVM)")]

    subgraph CON["Consumidores"]
        EV["evaluator<br/>SafeguardBlockEventListener<br/>⚠️ stub"]
        OV["oversight<br/>AlertGeneratedEventListener<br/>→ log + snapshot"]
    end

    SG --> BUS
    BRI --> BUS
    SEN --> BUS
    BRI -->|"SafeguardBlockEvent<br/>shared.events ✅"| EV
    BUS --> BRI
    SEN -->|"AlertGeneratedEvent<br/>⚠️ fuga de módulo"| OV
```

⚠️ Deuda conocida: listeners dentro de la transacción del publicador (falta
`AFTER_COMMIT`), evaluator no publica eventos, contratos incompletos en `shared.events`.

---

## 5. Flujos principales

### 5.1 Autenticación

```mermaid
sequenceDiagram
    actor U as Usuario
    participant FE as Angular
    participant API as /auth/*
    participant DB as PostgreSQL

    U->>FE: correo + contraseña (+ código de rol)
    FE->>API: POST /auth/register · /auth/login
    API->>DB: BCrypt · refresh_token (7 días)
    API-->>FE: access (15 min) + refresh (7 d)
    FE->>FE: tokens en localStorage + roleGuard
```

Roles → rutas: `ADMINISTRATIVO` (usuarios, códigos), `PROFESOR` (exámenes, oversight), `ALUMNO` (join por código).

### 5.2 Ciclo de examen (profesor → alumno → resultados)

```mermaid
flowchart TD
    P(["Profesor"]) --> C1["Crea examen + preguntas<br/>POST /api/evaluator/exams"]
    C1 --> C2["Publica (≥1 pregunta)<br/>POST .../publish"]
    C2 --> C3["Código de acceso (expirable)<br/>POST .../access-code"]

    S(["Alumno"]) --> J["Ingresa con código<br/>POST /api/evaluator/attempts/join"]
    J --> A["Responde + autoguardado<br/>POST .../answers"]
    A --> SU["Entrega<br/>POST .../submit"]
    SU --> RES["Resultados / reviews<br/>GET .../result · /results · /reviews"]

    C3 -.código.-> J
```

### 5.3 SafeGuard en el intento

```mermaid
flowchart LR
    CFG["Profesor configura<br/>PUT /api/safeguard/exams/{id}/config"] --> ST["Alumno: sessions/start<br/>→ sessionId + config"]
    ST --> LP{"Durante el intento<br/>(loop)"}
    LP -->|"tab · blur · copy · atajo"| EV["POST /api/safeguard/events"]
    LP -->|"cada X s"| HB["POST .../heartbeat"]
    EV --> POL["SafeguardRiskPolicy<br/>puntos por evento"]
    POL --> CHK{"riskScore ≥ threshold?"}
    CHK -->|"sí"| FL["FLAGGED / TERMINATED"]
    CHK -->|"no"| OK["ACTIVE"]
    POL --> EVT["RiskEventRecorded"] --> BLO["SafeguardBlockEvent"]
    BLO --> STUB["evaluator listener (stub)"]
    LP --> CL["Al entregar: sessions/close"]
```

Violaciones detectadas en el frontend por `SafeGuardMonitorService`:
`visibilitychange`, `blur`, `copy/paste/cut`, menú contextual, atajos (F12, Ctrl+Shift+I…).

### 5.4 Sentinel (STOMP) y Oversight (polling)

```mermaid
flowchart LR
    subgraph Sentinel
        WSC["/ws/sentinel<br/>POST destino /app/sentinel/events"] --> UC["receive(command)"]
        UC --> R["Redis ZADD<br/>ventana 5 min (dedup)"]
        UC --> PG8[("sentinel_events")]
        UC -->|"umbral alcanzado"| PG8b[("sentinel_alerts")]
        PG8b --> ALE["AlertGeneratedEvent"]
    end

    subgraph Oversight
        ALE --> LIS["AlertGeneratedEventListener"]
        LIS --> LOG[("oversight_log_entries")]
        LIS --> SNAP[("oversight_integrity_snapshots<br/>base 100 − penalizaciones")]
    end

    FE["oversight.page (3 pestañas)"] -->|"GET summary · timeline<br/>polling cada 30 s"| API["OversightController"]
    FE -->|"GET /attempts/{id}/integrity"| API
    API --> SNAP
```

Índice de integridad: `GREEN ≥ 80` · `YELLOW 50–79` · `RED < 50`.

Nota: ⚠️ **solo Sentinel alimenta oversight hoy**; SafeGuard y evaluator no (Sprint 7 pendiente).
No existe `/ws/oversight` ni cliente WebSocket en el frontend — todo es REST + polling.

---

## 6. Modelo de datos (Flyway V1..V10)

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
    exam_attempts ||--o| oversight_integrity_snapshots : scores
```

Migraciones: `V1` auth · `V2` códigos registro · `V3` evaluator · `V4` owner de bancos ·
`V5` seed México · `V6` timestamps review · `V7` safeguard · `V8` sentinel ·
`V9` oversight · `V10` puntos de pregunta + defaults safeguard. Nunca editar una `V*` aplicada.

---

## 7. Arquitectura hexagonal por módulo

```mermaid
flowchart TD
    subgraph Hexagonal["Cada módulo (hexagonal / ports-adapters)"]
        IN["adapters IN<br/>Controller · WebSocket · EventListener"] --> APP["application<br/>use-cases · DTOs · listeners"]
        APP --> DOM["domain<br/>modelos · políticas · puertos"]
        APP --> OUT["adapters OUT<br/>JPA · Redis · publisher"]
        OUT --> DBX[("PostgreSQL / Redis")]
    end
    Note1["auth · evaluator · safeguard: application/domain/infrastructure"]
    Note2["sentinel · oversight: ports / adapters(in|out) / application / domain"]
```

Gotchas del código: MapStruct está en el `pom.xml` pero **no se usa** (mappers
manuales); `ddl-auto: validate` — el esquema viene solo de Flyway.

---

## 8. Stack y pipelines

```mermaid
flowchart LR
    subgraph CI[".github/workflows/ci.yml (push/PR → main)"]
        A["mvnw verify<br/>(352 tests)"] --> B1["docker build backend"]
        C2["npm ci"] --> D2["npm run lint"] --> E2["build producción<br/>(www/)"] --> F2["docker build frontend"]
        B1 & F2 --> G["Despliegue Oracle Cloud / VPS<br/>Nginx reverse proxy"]
    end
```

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 4.1 · Java 25 · Maven wrapper |
| BD | PostgreSQL 16 (Flyway, `ddl-auto: validate`) |
| Cache | Redis 7 (ventana deslizante de eventos) |
| Auth | JWT (access 15 min, refresh 7 d, rotación, rate-limit) |
| Realtime | STOMP WS (`/ws/sentinel`, interceptor JWT) — sin cliente FE |
| Frontend | Angular 22 zoneless · Ionic 9 · Bootstrap 5 · Vitest · Tauri v2 |
| Testing | JUnit 5 + Testcontainers (Pg+Redis) · Vitest |

---

## 9. Riesgos y pendientes (mapa de calor)

```mermaid
quadrantChart
    title Pendientes: impacto vs. esfuerzo
    x-axis Esforzo bajo --> Esforzo alto
    y-axis Impacto bajo --> Impacto alto
    quadrant-1 Planear bien
    quadrant-2 Prioridad alta
    quadrant-3 Rellenos
    quadrant-4 Quick wins
    "N+1 GetExamResult": [0.65, 0.85]
    "AFTER_COMMIT en listeners": [0.55, 0.8]
    "Contratos shared.events unificados": [0.75, 0.7]
    "WS cliente + /ws/oversight": [0.8, 0.6]
    "Autosave Redis evaluator (Sprint 6)": [0.7, 0.65]
    "Sprint 7 integración completa": [0.85, 0.9]
    "IDs sentinel vs JWT principal": [0.35, 0.7]
    "WS origin *": [0.2, 0.6]
    "Refresh token en claro": [0.5, 0.75]
    "Ruta /exams/:id/take muerta": [0.2, 0.2]
    "Sonar backlog MAJOR (S6819 x20)": [0.3, 0.15]
    "Docs legales (privacidad/ARCO)": [0.4, 0.55]
```

Top limitaciones MVP: sin bloqueo OS real · sin apps móviles · sin WS en
frontend · sin autosave Redis · refresh tokens en claro en localStorage ·
`metadata` JSON válido o 500 · 4 tipos de evento sin regla de riesgo.

---

## 10. Dónde está todo

```mermaid
flowchart TD
    ROOT["repo"] --> BKC["integrity-core/"]
    ROOT --> FRC["integrity-core-fr/"]
    ROOT --> DC["docker-compose.yml"]
    ROOT --> CI[".github/workflows/ci.yml"]

    BKC --> SRC["src/main/java"]
    SRC --> MODS["auth · evaluator · sentinel ·<br/>safeguard · oversight · shared_kernel · config"]
    SRC --> EVT["shared/events/SafeguardBlockEvent"]
    BKC --> MIG["resources/db/migration/V1..V10"]

    FRC --> APPR["src/app"]
    APPR --> PAGES["11 pages: login · register · home · exam-list ·<br/>exam-editor · exam-results · exam-reviews ·<br/>exam-result · exam-attempt · exam-take ·<br/>oversight · evaluator-demo"]
    APPR --> SGM["application/safeguard/safeguard-monitor.service.ts"]
    FRC --> DS["DESIGN-SYSTEM.md"]
    FRC --> TA["src-tauri/src/lib.rs<br/>5 comandos nativos"]

    ROOT --> DOC["README.md · FLOW.md · ToDo.md ·<br/>MEMORY.md · AGENTS.md · docs/"]
```
