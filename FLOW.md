# CIDEM Integrity - Project Flow Documentation

## Overview

This document describes the complete flow of the CIDEM Academic Integrity Platform, covering the backend (Spring Boot 4.1, Java 25) and frontend (Angular 22 + Ionic 9) with Tauri v2 for desktop demo capabilities.

## Architecture

### Backend Modules (Hexagonal/Ports-Adapters)

```
integrity-core/
├── auth/                 # Authentication & Authorization (JWT, roles, registration)
├── evaluator/            # Exam management, attempts, answers, sessions (Redis)
├── sentinel/             # Suspicious events detection, alerting, WebSocket (/ws/sentinel)
├── safeguard/            # Exam restrictions config, session monitoring, block events
├── oversight/            # Teacher dashboard, integrity index, timeline, WebSocket (/ws/oversight)
├── shared_kernel/        # Shared exceptions, utilities (placeholder)
├── shared/events/        # Cross-module event contracts
└── config/               # Redis, CORS, security configuration
```

### Frontend Structure

```
integrity-core-fr/
├── src/
│   ├── app/
│   │   ├── domain/
│   │   │   ├── models/           # Domain models (Exam, User, Safeguard, Oversight, etc.)
│   │   │   └── ports/            # Repository interfaces (ports)
│   │   ├── application/
│   │   │   ├── use-cases/        # Use cases organized by module
│   │   │   └── safeguard/        # SafeGuard monitor, dispatcher, shortcuts
│   │   ├── infrastructure/
│   │   │   ├── http/             # HTTP repository implementations
│   │   │   ├── platform/         # Web Safeguard platform implementation
│   │   │   ├── storage/          # Token storage
│   │   │   └── interceptors/     # Auth, refresh interceptors
│   │   ├── presentation/
│   │   │   ├── pages/            # Ionic pages (lazy loaded)
│   │   │   └── guards/           # Role-based route guards
│   │   └── environments/         # Environment configs
│   └── src-tauri/                # Tauri v2 desktop app
```

---

## Core Flows

### 1. Authentication Flow

```
User Registration:
  1. POST /api/auth/register (with registration code)
  2. Validate code (ADMIN/PROFESOR/ALUMNO)
  3. Hash password (BCrypt)
  4. Save user + return JWT tokens

User Login:
  1. POST /api/auth/login
  2. Validate credentials
  3. Generate access token (15min) + refresh token (7 days)
  4. Return tokens + user info

Token Refresh:
  1. POST /api/auth/refresh
  2. Validate refresh token
  3. Issue new access token
```

**Roles**: ADMINISTRATIVO, PROFESOR, ALUMNO

---

### 2. Exam Creation Flow (Professor)

```
1. POST /api/evaluator/exams
   - Title, description, duration, questions (with options)
2. Exam created with status DRAFT
3. Professor can edit while DRAFT
4. POST /api/evaluator/exams/{id}/publish → status PUBLISHED
5. POST /api/evaluator/exams/{id}/access-code → generates code for students
6. Students join with code via POST /api/evaluator/exams/join
```

---

### 3. Student Exam Taking Flow

```
1. Student enters access code → POST /api/evaluator/exams/join
2. Creates ExamAttempt (status IN_PROGRESS)
3. Frontend: ExamAttemptPage loads questions
4. SafeGuardMonitor.start(examId, attemptId) → activates protection
5. Student answers questions (auto-save to Redis session)
6. Student submits → POST /api/evaluator/exams/{id}/attempts/{attemptId}/submit
7. SafeGuardMonitor.stop() → closes SafeGuard session
8. Evaluator publishes ExamSessionFinishedEvent
9. Oversight calculates final integrity index
```

**SafeGuard Protection** (active during exam):
- Tab switch detection (visibilitychange)
- Window blur detection
- Copy/Paste/Cut blocking
- Context menu blocking
- Restricted keyboard shortcuts (F12, Ctrl+Shift+I, etc.)
- All violations → POST /api/safeguard/events → SafeguardBlockEvent

---

### 4. SafeGuard Monitoring Flow

```
Configuration (Professor):
  PUT /api/safeguard/exams/{examId}/config
  - enable/disable restrictions
  - riskThreshold, autoTerminateOnThreshold

Student Session:
  1. POST /api/safeguard/sessions/start (examId, attemptId, userAgent)
  2. Returns sessionId, config, initial riskScore=0
  3. Frontend heartbeat every 30s → POST /api/safeguard/sessions/{id}/heartbeat
  4. Violations → POST /api/safeguard/events (sessionId, eventId, type, metadata)
  5. Backend: SafeguardRiskPolicy calculates points
  6. If riskScore ≥ threshold → status FLAGGED/TERMINATED
  7. Publishes SafeguardBlockEvent (shared/events)
  8. POST /api/safeguard/sessions/{id}/close on exam end
```

---

### 5. Sentinel Event Processing Flow

```
1. Client (or SafeGuard) sends suspicious event:
   POST /api/sentinel/events (or WebSocket /app/sentinel/events)

2. ReceiveSuspiciousEventService:
   - Check deduplication (5 min window per attempt+type)
   - Save SuspiciousEvent to PostgreSQL
   - Record in Redis sorted set (EventWindowPort)
   - Check escalation policy (2 min window)
   - If threshold met → create Alert
   - Save Alert to PostgreSQL
   - Publish SentinelAlertEvent

3. Alert escalation:
   - Count events in window
   - LOW: 1-2 events, MEDIUM: 3-4, HIGH: 5+
   - Auto-escalates based on frequency
```

---

### 6. Oversight Real-Time Monitoring Flow (Teacher Dashboard)

```
WebSocket Connection:
  1. Teacher connects to /ws/oversight (JWT auth)
  2. Subscribes to /topic/exams/{examId}/timeline
  3. Subscribes to /topic/exams/{examId}/integrity/{attemptId}
  4. Subscribes to /topic/exams/{examId}/summary

Event Broadcasting (automatic via @EventListener):
  - SafeguardBlockEvent → AlertGeneratedEventListener → broadcastLogEntry
  - SentinelAlertEvent → SafeguardBlockEventListener → broadcastLogEntry
  - EvaluatorStateEvent → EvaluatorStateEventListener → broadcastLogEntry
  - ExamSessionStartedEvent → EvaluatorStateEventListener → broadcastLogEntry
  - ExamSessionFinishedEvent → EvaluatorStateEventListener → broadcastLogEntry

REST Endpoints (for initial load):
  GET /api/oversight/exams/{examId}/summary
  GET /api/oversight/exams/{examId}/timeline
  GET /api/oversight/attempts/{attemptId}/integrity

Integrity Index Calculation:
  Base: 100
  - 30 points: Tab/app switch
  - 15 points: Repeated focus loss
  - 25 points: SafeGuard block
  - 10 points: Prolonged disconnect
  Risk Levels: GREEN (≥80), YELLOW (50-79), RED (<50)
```

---

### 7. Redis Session Management & Autosave (Evaluator)

```
Session Lifecycle:
  1. Exam starts → ExamSessionService.createSession()
     - Stores in Redis: evaluator:session:{attemptId} (TTL 4h)
     - Contains: attemptId, examId, studentId, timeRemaining, answers, status

  2. Auto-save (front-end periodic):
     - PUT /api/evaluator/sessions/{attemptId}/answers/{questionId}
     - PUT /api/evaluator/sessions/{attemptId}/time (remaining seconds)
     - PUT /api/evaluator/sessions/{attemptId}/question (current index)
     - PUT /api/evaluator/sessions/{attemptId}/activity (heartbeat)

  3. Recovery on reconnect:
     - GET /api/evaluator/sessions/{attemptId}
     - Restore answers, time, position
     - Continue exam seamlessly

  4. Exam submit:
     - PUT /api/evaluator/sessions/{attemptId}/submit → status SUBMITTED
     - Session preserved for review
```

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
  "payload": { ... }
}
```

**Events:**
| Event | Source | Consumers | Purpose |
|-------|--------|-----------|---------|
| SafeguardBlockEvent | Safeguard | Oversight, Evaluator | Restriction violations |
| SentinelAlertEvent | Sentinel | Oversight, Evaluator | Suspicious activity alerts |
| EvaluatorStateEvent | Evaluator | Oversight | Answer save, state changes |
| ExamSessionStartedEvent | Evaluator | Oversight | Exam begins |
| ExamSessionFinishedEvent | Evaluator | Oversight | Exam ends |

---

### 9. Tauri Desktop Demo Capabilities

**Native Commands (SafeGuard integration):**
- `get_system_info()` - OS, arch, hostname
- `show_notification(title, body)` - Desktop notifications
- `block_keyboard_shortcut(shortcut)` - OS-level shortcut blocking
- `get_active_window_title()` - Active window monitoring
- `take_screenshot()` - Screen capture for proctoring

**Security CSP:**
```
default-src 'self' ipc: http://localhost:8080 ws://localhost:8080
script-src 'self' 'unsafe-inline' 'unsafe-eval'
connect-src 'self' http://localhost:8080 ws://localhost:8080
```

---

## Data Flow Summary

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Student   │────▶│  Evaluator  │────▶│  SafeGuard  │
│  (Frontend) │     │  (Exam)     │     │  (Monitor)  │
└─────────────┘     └──────┬──────┘     └──────┬──────┘
                           │                   │
                    ┌──────▼──────┐     ┌──────▼──────┐
                    │   Redis     │     │  PostgreSQL │
                    │  (Session)  │     │  (Config,   │
                    └─────────────┘     │   Events)   │
                                        └──────┬──────┘
                                               │
                    ┌─────────────┐     ┌──────▼──────┐
                    │  Sentinel   │◀────│  Oversight  │
                    │ (Events)    │     │  (Dashboard)│
                    └─────────────┘     └─────────────┘
                           │                   │
                    ┌──────▼──────┐     ┌──────▼──────┐
                    │  PostgreSQL │     │  WebSocket  │
                    │  (Alerts)   │     │  (Real-time)│
                    └─────────────┘     └─────────────┘
```

---

## Deployment

### Docker Compose (Local/Production)
```yaml
services:
  db:       PostgreSQL 16
  redis:    Redis 7
  backend:  Spring Boot (port 8080)
  frontend: Angular/Ionic (port 8081)
```

### Environment Variables (Backend)
- `JWT_SECRET` (≥32 chars)
- `ADMIN_REGISTRATION_CODE`
- `PROFESOR_REGISTRATION_CODE`
- `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`
- `CORS_ALLOWED_ORIGINS`

### CI/CD Pipeline
1. Backend: `./mvnw verify` (tests, build, Docker)
2. Frontend: `npm ci → lint → build:production → Docker`
3. Deploy: Oracle Cloud / VPS with Nginx reverse proxy
4. Healthcheck: `/actuator/health`

---

## Key Technologies

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 4.1, Java 25, Maven |
| Database | PostgreSQL 16 (Flyway migrations) |
| Cache/Sessions | Redis 7 |
| Auth | JWT (access 15min, refresh 7d) |
| Real-time | STOMP over WebSocket |
| Frontend | Angular 22, Ionic 9, TypeScript 6 |
| Desktop | Tauri v2, Rust |
| Testing | JUnit 5, Testcontainers, Vitest |
| Build | Maven, npm, Docker |

---

## Testing Strategy

- **Unit Tests**: Domain logic, policies, mappers (fast, no containers)
- **Integration Tests**: SpringBootTest + Testcontainers (PostgreSQL, Redis)
- **WebSocket Tests**: STOMP client simulation
- **Frontend**: Vitest (unit), manual E2E for demo
- **All 344 backend tests passing**

---

## Known Limitations (MVP)

1. No full OS-level lockdown (Tauri demo only shows capabilities)
2. No mobile apps (Android/iOS)
3. No advanced reporting/analytics
4. No Grafana/Prometheus monitoring
5. No RabbitMQ/event bus (uses Spring ApplicationEventPublisher)
6. No high availability/microservices (modular monolith)
7. SafeGuard metadata field accepts JSON only (500 on invalid)
8. Four event types without policy rules: DEVTOOLS_OPENED, CAMERA_BLOCKED, MICROPHONE_ANOMALY, SCREEN_SHARE_STOPPED

---

## Sprint Progress

| Sprint | Status | Deliverable |
|--------|--------|-------------|
| 1 | ✅ | Base integration, shared events, contracts |
| 2 | ✅ | Sentinel REST, persistence, deduplication |
| 3 | ✅ | Oversight MVP (backend + frontend panel) |
| 4 | ✅ | WebSockets real-time monitoring |
| 5 | ✅ | SafeGuard backend + frontend + Tauri demo |
| 6 | ✅ | Redis sessions, autosave, recovery |
| 7 | 🔄 | Full integration test flow |
| 8 | ⏳ | Release candidate, deploy, docs |

---

## File Locations

### Backend Key Files
- `integrity-core/src/main/java/.../shared/events/*.java` - Event contracts
- `integrity-core/src/main/java/.../sentinel/adapters/in/websocket/` - Sentinel WebSocket
- `integrity-core/src/main/java/.../oversight/adapters/in/websocket/` - Oversight WebSocket
- `integrity-core/src/main/java/.../evaluator/domain/model/ExamSession.java` - Redis session model
- `integrity-core/src/main/resources/db/migration/V*.sql` - Flyway migrations

### Frontend Key Files
- `integrity-core-fr/src/app/presentation/pages/oversight/` - Oversight dashboard
- `integrity-core-fr/src/app/application/safeguard/safeguard-monitor.service.ts` - SafeGuard monitor
- `integrity-core-fr/src/app/presentation/pages/exam-attempt/` - Student exam with SafeGuard
- `integrity-core-fr/src-tauri/src/lib.rs` - Tauri native commands

---

*Generated: 2026-10-07*