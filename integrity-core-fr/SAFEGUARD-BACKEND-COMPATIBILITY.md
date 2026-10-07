# SafeGuard frontend ↔ backend compatibility

The SafeGuard frontend adapter is aligned with the backend under `/api/safeguard`.

## HTTP contract

- `GET /api/safeguard/exams/{examId}/config`
- `POST /api/safeguard/sessions/start`
- `POST /api/safeguard/events`
- `POST /api/safeguard/sessions/{sessionId}/heartbeat`
- `POST /api/safeguard/sessions/{sessionId}/close`

## Event mapping

| Frontend monitor event | Backend event type |
|---|---|
| `TAB_HIDDEN` | `TAB_SWITCH` |
| `WINDOW_BLUR` | `WINDOW_BLUR` |
| `COPY_ATTEMPT` | `COPY_PASTE` |
| `CUT_ATTEMPT` | `COPY_PASTE` |
| `PASTE_ATTEMPT` | `COPY_PASTE` |
| `CONTEXT_MENU` | `RIGHT_CLICK` |
| `RESTRICTED_SHORTCUT` | `SUSPICIOUS_ACTIVITY` |

The original frontend event type, result (`BLOCKED`/`DETECTED`) and detail are preserved in the backend `metadata` JSON string.

## Configuration mapping

The backend exposes one `blockCopyPaste` flag, so the web monitor applies it to copy, cut and paste. The browser-only controls for context menu and restricted shortcuts remain enabled by the frontend because the current backend configuration does not expose separate flags for them.

`eventThrottleMs` and the client heartbeat interval remain frontend policy values because the current backend configuration DTO does not expose those settings.

## Session safety

The monitor no longer sends an event before a valid backend `sessionId` exists. Events that happen while `/sessions/start` is still pending are held in a small in-memory pre-session queue and are sent as soon as the session starts.

## Respuestas del backend que el monitor maneja

| Situación | Respuesta del backend | Comportamiento del front |
|---|---|---|
| Examen sin configuración de SafeGuard | `GET /config` → 404 | El monitor se detiene: no bloquea nada ni abre sesión (igual que si estuviera desactivado). |
| SafeGuard desactivado para el examen | `enabled: false` | El monitor se detiene. |
| Error de red / 5xx al leer la config | — | Protege con la política estricta por defecto y reintenta abrir la sesión en cada latido. |
| `sessions/start` rechazado (400 / 403 / 404) | Intento inexistente, ajeno o petición inválida | La protección local sigue activa, pero no se reintenta abrir la sesión. |
| `sessions/start` falla por red, 5xx o 409 de concurrencia | — | Se reintenta en el siguiente latido. |
| Evento rechazado (otro 4xx, p. ej. 409 sesión cerrada) | — | Se descarta para no bloquear la cola. |

Limitación conocida: el front ignora `closed` / `status` de las respuestas de heartbeat y eventos, así que si el backend termina la sesión por `autoTerminateOnThreshold`, la pantalla del examen no se bloquea por sí sola.

## Run locally

Keep the existing `environment.apiUrl` root value, for example:

```ts
apiUrl: 'http://localhost:8080'
```

The SafeGuard repository itself appends `/api/safeguard`.

The authenticated frontend must send a valid JWT, and the backend must have a SafeGuard configuration for the exam before a student starts the protected attempt.

## Validation performed

- TypeScript application compile: passed.
- TypeScript spec compile: passed.
- Angular/Vitest: 33 tests passed (3 spec files). Se ejecutaron con el chequeo de versión de Node del CLI desactivado, porque el entorno tiene Node 22.22.2 y el CLI exige 22.22.3+.
- Angular template compile (`ngc`) y ESLint: passed.
- Not executed: pruebas contra el backend levantado (requiere Java 25, PostgreSQL y Redis).
