# Módulo de SafeGuard — `integrity_core.safeguard`

Control de integridad durante un examen en vivo: configuración de restricciones por examen,
sesiones de vigilancia, registro de eventos de riesgo con puntaje acumulado y publicación
del contrato compartido de bloqueos hacia EVALUATOR y OVERSIGHT.

Estructura hexagonal idéntica a `auth` (reestructurado el 2026-10-01; ver `MEMORY.md`).

---

## 1. Arquitectura

```
safeguard/
├── domain/                     Sin frameworks (ni JPA, ni Spring Security)
│   ├── model/                  SafeguardConfiguration, SafeguardSession, SafeguardEvent,
│   │                           SafeguardEventType, SafeguardSessionStatus
│   ├── event/                  SafeguardDomainEvent (eventos internos, sealed interface)
│   ├── exception/              SafeguardException (NOT_FOUND, FORBIDDEN, INVALID_STATE…)
│   ├── service/                SafeguardRiskPolicy (severidad y puntos por tipo de evento)
│   └── port/                   ExamAttemptPort, SafeguardConfigurationRepository,
│                               SafeguardSessionRepository, SafeguardEventRepository
│
├── application/
│   ├── usecase/                SafeguardService (startSession, recordEvent, heartbeat,
│   │                           closeSession, getConfiguration, upsertConfiguration…)
│   ├── dto/                    StartSessionCommand, RecordEventCommand, UpsertConfigCommand,
│   │                           ConfigView, SessionView, RecordEventResult, HeartbeatResult
│   └── listener/               SafeguardBlockEventBridge (interno → contrato compartido)
│
└── infrastructure/
    ├── persistence/
    │   ├── entity/             SafeguardConfigurationEntity, SafeguardSessionEntity,
    │   │                       SafeguardEventEntity (@Entity — única capa con JPA)
    │   ├── port/               *JpaRepository (Spring Data)
    │   ├── mapper/             Mappers manuales dominio ↔ entidad
    │   └── adapter/            *RepositoryImpl (implementan domain/port) +
    │                           JdbcExamAttemptAdapter (JDBC sobre exam_attempts/exams)
    ├── security/               CurrentStudentProvider (puerto) + SecurityCurrentStudentProvider
    └── web/
        ├── controller/         SafeguardController
        ├── dto/                Requests/Responses de la API
        └── exception/          SafeguardExceptionHandler (ProblemDetail + `code`)
```

**Regla de dependencia:** `domain` no importa nada de `infrastructure`. `application` solo
conoce `domain`. `infrastructure` es la única capa que conoce Spring, JPA y seguridad.

Los mappers son clases escritas a mano (estilo `auth`): el `pom.xml` tiene MapStruct pero
ningún módulo lo usa.

---

## 2. Modelo de dominio

| Clase | Tipo | Descripción |
|---|---|---|
| `SafeguardConfiguration` | Entidad | Configuración por examen (1:1, `exam_id` único): flags de cámara/micrófono/pantalla completa/copy-paste, `maxTabSwitches`, `riskThreshold`, `autoTerminateOnThreshold`. |
| `SafeguardSession` | Entidad | Sesión de vigilancia de un intento (`attempt_id` único). Estado `PENDING → ACTIVE → COMPLETED/FLAGGED/TERMINATED`, `riskScore` acumulado, heartbeat. Referencia a su configuración. |
| `SafeguardEvent` | Entidad | Evento de riesgo registrado (idempotente por `event_id` único por sesión). Puntos de riesgo + metadatos JSON. |
| `SafeguardEventType` | Enum | Tipos de evento (tab switch, copy/paste, fullscreen…), cada uno con severidad y puntos según `SafeguardRiskPolicy`. |
| `SafeguardSessionStatus` | Enum | `PENDING`, `ACTIVE`, `PAUSED`, `COMPLETED`, `FLAGGED`, `TERMINATED`. |
| `SafeguardDomainEvent` | Sealed interface | Eventos internos: `SessionStarted`, `RiskEventRecorded`, `SessionStatusChanged`, `SessionClosed`. Solo se publican dentro del módulo. |

---

## 3. Endpoints

Base path: `/api/safeguard`. Autenticación JWT (cualquier rol salvo indicación).

| Método | Ruta | Rol requerido | Descripción |
|---|---|---|---|
| GET | `/api/safeguard/exams/{examId}/config` | `PROFESOR`/`ADMINISTRATIVO` (cualquier examen) o `ALUMNO` **con intento en ese examen** | Lee la configuración. Sin configuración → 404. |
| PUT | `/api/safeguard/exams/{examId}/config` | `PROFESOR`/`ADMINISTRATIVO` | Crea o reemplaza la configuración (upsert). Examen inexistente → 404; `maxTabSwitches < 0` o `riskThreshold <= 0` → 400. |
| POST | `/api/safeguard/sessions/start` | Autenticado (alumno) | Inicia la sesión de vigilancia de su intento. Devuelve la sesión **con las banderas de restricción** (los alumnos leen la config por aquí). |
| POST | `/api/safeguard/events` | Autenticado (dueño de la sesión) | Registra un evento de riesgo. 201 nuevo / 200 duplicado (misma `eventId`). |
| POST | `/api/safeguard/sessions/{sessionId}/heartbeat` | Autenticado (dueño) | Renueva el heartbeat y devuelve estado + riesgo acumulado. |
| POST | `/api/safeguard/sessions/{sessionId}/close` | Autenticado (dueño) | Cierra la sesión (idempotente). |

> **Cambio de ruta (2026-10-01):** la lectura de configuración pasó de
> `GET /api/safeguard/config/{examId}` a `GET /api/safeguard/exams/{examId}/config`
> para alinearla con la convención `/api/safeguard/exams/{examId}/...` del ToDo.
> No había consumidores (el frontend aún no llama a SafeGuard).

La autorización de rol vive en el controlador (`@PreAuthorize` para escritura; la regla de
lectura de alumno —debe tener intento— se evalúa en el controlador contra el servicio).
El patrón de roles por cadena de autoridades es el mismo que `EvaluationReviewController`.

### Ejemplo de configuración

```json
{
  "enabled": true,
  "requireCamera": true,
  "requireMicrophone": false,
  "requireFullscreen": true,
  "blockCopyPaste": true,
  "maxTabSwitches": 3,
  "riskThreshold": 100,
  "autoTerminateOnThreshold": false
}
```

---

## 4. Concurrencia e idempotencia (contrato crítico)

- **`recordEvent`** carga la sesión con bloqueo pesimista (`findByIdForUpdate`,
  `PESSIMISTIC_WRITE`) dentro de la transacción: reintentos concurrentes sobre la misma
  sesión se serializan, y además la unicidad `(session_id, event_id)` en BD corta los
  duplicados (una misma `eventId` reprocesada responde 200 sin sumar riesgo dos veces).
- El adapter **reutiliza la entidad gestionada** del contexto de persistencia al guardar
  (nunca una instancia desacoplada): así el `UPDATE` conserva `id`/`created_at` y la
  columna optimista `version`, y el bloqueo pesimista sigue siendo válido. No hacer esto
  produce `OptimisticLockException` (falla una vez que hay una `version` real en la fila).
- `SafeguardSessionRepository.saveAndFlush` / `SafeguardEventRepository.saveAndFlush` son
  los únicos métodos de escritura de los puertos; los nombres se mantuvieron para no
  cambiar el código del caso de uso.

---

## 5. Eventos y contrato compartido

```
SafeguardService ──publica──▶ SafeguardDomainEvent.RiskEventRecorded   (interno)
                                     │
                        SafeguardBlockEventBridge (@EventListener, application/listener)
                                     │ ──publica──▶ SafeguardBlockEvent  (contrato compartido)
                                     ▼
             evaluator/.../SafeguardBlockEventListener   ← vacío (Sprint 7 lo implementa)
             oversight                                   ← sin consumer (bloqueado, ver §8)
```

- **Contrato:** `edu.cidemags.integrity_core.shared.events.SafeguardBlockEvent` —
  record con los campos estándar del ToDo (Sprint 1): `eventId`, `examId`, `attemptId`,
  `examSessionId`, `studentId`, `occurredAt`, `source` (= `"safeguard"`), `payload`
  (mapa con `eventType`, `severity`, `riskPoints`, `totalRiskScore`).
- Solo `RiskEventRecorded` se convierte en bloqueo; `eventId` es el UUID del evento de
  riesgo (único por sesión) para que los consumidores dedupliquen.
- El paquete compartido es `shared.events` (decisión sobre `shared_kernel`, que queda
  reservado para excepciones/utilidades comunes). Ver `AGENTS.md`.
- Los eventos se publican de forma síncrona dentro de la transacción; los consumidores
  que escriban en BD deberían usar `@TransactionalEventListener` (hoy no hay ninguno).

---

## 6. Tablas (migración V7) y propiedad

| Tabla | Dueño | Notas |
|---|---|---|
| `safeguard_configuration` | safeguard | FK a `exams(id)`. CHECKs: `max_tab_switches >= 0`, `risk_threshold > 0`. |
| `safeguard_session` | safeguard | FKs a configuración, `exams`, `exam_attempts`, `users`. CHECKs de estado/riesgo/fechas. |
| `safeguard_event` | safeguard | FK a sesión. `event_id` único por sesión. Metadatos `jsonb`. |
| `exams`, `exam_attempts` | evaluator | **SafeGuard las lee** por JDBC (`JdbcExamAttemptAdapter`) en vez de cruzar módulos por Java — dependencia de tablas ajenas conocida, ver §8. |

`ddl-auto: validate` — el esquema solo viene de Flyway; nunca se edita una `V*` aplicada.

---

## 7. Tests

| Capa | Archivo | Qué cubre |
|---|---|---|
| Casos de uso | `SafeguardServiceTest` (31) | startSession/recordEvent/heartbeat/closeSession + `upsertConfiguration` + `assertStudentMayReadConfiguration`, con mocks de los puertos (Mockito, estricto). |
| Web | `SafeguardControllerTest` (29, `@WebMvcTest`) | Cada endpoint: 200/201/400/403/404/409, validación de bodies, mapeo de comandos, incluido `heartbeat` (200, body ausente soportado, 400 sin `clientTime`, 404/403). |
| Autorización | `SafeguardConfigAuthorizationTest` (8, `@WebMvcTest` + `@Import(SecurityConfig)` + `@WithMockUser`) | Regla aprobada: PUT acepta `PROFESOR`/`ADMINISTRATIVO` y rechaza 403 a `ALUMNO`/sin autenticar; GET de `ALUMNO` 200 con intento y 403 sin intento. |
| Política de riesgo | `SafeguardRiskPolicyTest` (19) | Tabla de severidad/puntos de los 12 tipos soportados (valores esperados escritos a mano), escalada de `TAB_SWITCH` sobre `maxTabSwitches` → severidad 4, y caracterización de los 4 tipos del enum sin regla (lanzan `IllegalArgumentException`). |
| Mappers | `SafeguardConfigurationMapperTest` (5), `SafeguardSessionMapperTest` (3), `SafeguardEventMapperTest` (5) | Mapeo entity↔dominio completo, `null → null`, y el contrato `updateEntity`: solo campos de negocio, nunca `id`/`createdAt`/`updatedAt`/`version`/relaciones sobre la instancia gestionada. |
| Adaptadores | `SafeguardConfigurationRepositoryImplTest` (5), `SafeguardSessionRepositoryImplTest` (7), `SafeguardEventRepositoryImplTest` (6) | Con JPA mockeado (Mockito estricto): ramas nueva-vs-gestionada de `saveAndFlush` (reutiliza la fila, conserva `@Version`), `IllegalStateException` si falta la fila gestionada, y resolución de asociaciones por `getReferenceById` (proxy por id). |
| Contrato de eventos | `SafeguardBlockEventFlowTest` (2) | Publicación/recepción real vía `AnnotationConfigApplicationContext`: `RiskEventRecorded` → puente → `SafeguardBlockEvent` → listener de EVALUATOR con todos los campos del contrato; eventos que no son riesgo no se reenvían. |
| Integración | `SafeguardConfigIntegrationTest` (5, `@SpringBootTest` + Testcontainers) | Persistencia real de configuración contra Postgres: INSERT (generación de UUID), UPDATE con `@Version`, alta/lectura/actualización por HTTP, 404/400 sin insertar, y la regla de lectura de alumno con intento real. |
| Integración | `SafeguardSessionIntegrationTest` (9, `@SpringBootTest` + Testcontainers, **no transaccional**) | Ciclo de vida completo de sesión contra Postgres real (esquema V7 de punta a punta): umbral → FLAGGED con heartbeat y cierre (FLAGGED no se degrada), auto-terminate → TERMINATED y 409, doble start idempotente (1 fila), `eventId` duplicado (1 fila, sin doble conteo), 403/404/409 sin persistir, y **2 tests `@Tag("concurrency")`**: N `eventId`s distintos en paralelo suman el score exacto (sin incrementos perdidos) y el mismo `eventId` en paralelo deja 1 fila + un `duplicate=true`. |

```bash
./mvnw -B --no-transfer-progress verify   # todo (requiere Docker/Podman para los 2 tests de contenedor)
./mvnw test -Dtest='Safeguard*'            # solo SafeGuard
```

---

## 8. Pendientes / mejoras futuras (no implementadas)

- **Consumer en OVERSIGHT de `SafeguardBlockEvent`**: bloqueado — `oversight` es un
  scaffold vacío sin código Java (solo READMEs). Definir también si consume con
  `@TransactionalEventListener` y qué persiste.
- **Listener de EVALUATOR vacío**: `SafeguardBlockEventListener` solo captura el último
  evento para las pruebas; la lógica de "actualiza estado del intento" es del Sprint 7.
- **Campo `resultado`** (¿una sesión terminada fue bloqueo o no?): hoy se representa con
  `status` + `totalRiskScore`. Si se consulta directamente en SQL/BI, proponer
  `V9__add_safeguard_result.sql` (CHECK sobre `status`); no se creó porque no había
  requerimiento de lectura.
- **`SessionStatusChanged` / `SessionClosed` no se reenvían** como `SafeguardBlockEvent`
  (solo los riesgos); si OVERSIGHT necesita el desenlace de la sesión, extender el puente.
- **Dependencia de tablas de evaluator**: `JdbcExamAttemptAdapter` consulta `exams` y
  `exam_attempts` por SQL crudo (Sprint 1: definir dueño de cada tabla — hoy evaluator).
  Alternativa futura: evento `AttemptStarted` o API interna de módulo.
- **Detalle de sesión en el flujo de riesgo**: `recordEvent` recarga la configuración por
  sesión para aplicar la política; con muchas sesiones activas esto es un SELECT extra por
  evento (sin problema práctico hoy, cachear en la sesión si crece).
- **`metadata` como String sobre columna `jsonb`**: el DTO acepta cualquier texto ≤ 4000
  caracteres, pero Hibernate lo escribe con `SqlTypes.JSON`; si el cliente envía texto que
  no es JSON válido, el INSERT falla (500). Validar en el controller (p. ej. `@Pattern`
  JSON) o cambiar la columna a `text`.
- **4 tipos del enum sin regla ni columna**: `DEVTOOLS_OPENED`, `CAMERA_BLOCKED`,
  `MICROPHONE_ANOMALY` y `SCREEN_SHARE_STOPPED` existen en `SafeguardEventType` pero no
  tienen caso en `SafeguardRiskPolicy` (lanzan `IllegalArgumentException` → 400) y el
  CHECK de `V7` tampoco los admite. O quitarlos del enum o añadirles severidad + migración
  `V9__`; hoy la inconsistencia queda caracterizada en `SafeguardRiskPolicyTest`.
- **Sin ownership por profesor en la config**: cualquiera con rol `PROFESOR` puede
  configurar cualquier examen (la regla aprobada solo define rol, no propiedad). Si se
  requiere, validar `exams.created_by_user_id` contra el usuario autenticado.

---

## 9. Inventario de archivos del módulo

```
domain/model/            SafeguardConfiguration, SafeguardEvent, SafeguardEventType,
                          SafeguardSession, SafeguardSessionStatus
domain/event/            SafeguardDomainEvent
domain/exception/        SafeguardException
domain/service/          SafeguardRiskPolicy
domain/port/             ExamAttemptPort, SafeguardConfigurationRepository,
                          SafeguardSessionRepository, SafeguardEventRepository

application/dto/         StartSessionCommand, RecordEventCommand, UpsertConfigCommand,
                          ConfigView, SessionView, RecordEventResult, HeartbeatResult
application/usecase/     SafeguardService
application/listener/    SafeguardBlockEventBridge

infrastructure/persistence/entity/      SafeguardConfigurationEntity, SafeguardSessionEntity,
                                        SafeguardEventEntity
infrastructure/persistence/port/        SafeguardConfigurationJpaRepository,
                                        SafeguardSessionJpaRepository, SafeguardEventJpaRepository
infrastructure/persistence/mapper/      SafeguardConfigurationMapper, SafeguardSessionMapper,
                                        SafeguardEventMapper
infrastructure/persistence/adapter/     SafeguardConfigurationRepositoryImpl,
                                        SafeguardSessionRepositoryImpl, SafeguardEventRepositoryImpl,
                                        JdbcExamAttemptAdapter
infrastructure/security/                CurrentStudentProvider, SecurityCurrentStudentProvider
infrastructure/web/controller/          SafeguardController
infrastructure/web/dto/                 StartSessionRequest, RecordEventRequest, HeartbeatRequest,
                                        SaveConfigurationRequest, SessionResponse, ConfigResponse,
                                        RecordEventResponse, HeartbeatResponse
infrastructure/web/exception/           SafeguardExceptionHandler

compartido: shared/events/SafeguardBlockEvent.java

tests:  application/usecase/SafeguardServiceTest
        infrastructure/web/controller/SafeguardControllerTest
        infrastructure/web/controller/SafeguardConfigAuthorizationTest
        (fuera del módulo) integration/SafeguardConfigIntegrationTest,
        integration/SafeguardBlockEventFlowTest
```
