# Plan acelerado de 8 sprints

## Sprint 1 — Integración base y contratos

**Objetivo:** dejar definida la comunicación entre módulos y evitar que cada integrante implemente interfaces incompatibles.

### Backend

- Definir el contrato común de eventos:
  - `SafeguardBlockEvent`
  - `SentinelAlertEvent`
  - `EvaluatorStateEvent`
  - `ExamSessionStartedEvent`
  - `ExamSessionFinishedEvent`
- Crear un paquete compartido, por ejemplo:

```text
edu.cidemags.integrity_core.shared.events
```

- Estandarizar los campos:

```text
eventId
examId
attemptId
examSessionId
studentId
occurredAt
source
payload
```

- Definir los límites de cada módulo.
- Definir qué módulo es dueño de cada tabla.
- Crear listeners vacíos y pruebas de publicación/recepción.
- Corregir la diferencia entre `AlertGeneratedEvent` y el nombre previsto `SentinelAlertEvent`.

### EVALUATOR

- Confirmar el identificador único de sesión de examen.
- Relacionar `ExamAttempt` con los eventos de integridad.
- Publicar `EvaluatorStateEvent` al:
  - Iniciar examen.
  - Guardar respuesta.
  - Finalizar examen.
  - Desconectarse o reconectarse.

### DevOps

- Separar configuración local y producción.
- Crear `.env.example`.
- Eliminar secretos hardcodeados del Compose.
- Añadir healthcheck para el backend.

### Entregable

Un examen de prueba en el que se publique y consuma al menos un evento entre dos módulos.

---

## Sprint 2 — Sentinel funcional

**Objetivo:** convertir Sentinel de modelo de dominio en un módulo operativo.

### Backend

- Crear endpoints REST:

```text
POST /sentinel/events
GET  /sentinel/exams/{examId}/alerts
GET  /sentinel/attempts/{attemptId}/events
GET  /sentinel/sessions/{sessionId}/alerts
```

- Reemplazar:
  - `InMemoryAlertRepository`
  - `InMemorySuspiciousEventRepository`

  por persistencia PostgreSQL.

- Crear migración Flyway para:
  - Eventos sospechosos.
  - Alertas.
  - Asociación con examen/intento/alumno.
- Publicar `SentinelAlertEvent`.
- Implementar deduplicación de alertas.
- Añadir validación de permisos por profesor y alumno.

### Redis

- Guardar contadores temporales de eventos.
- Configurar ventana de tiempo para reglas de escalamiento.
- Definir TTL de los contadores.

### Frontend

- Crear pantalla básica de alertas para profesores.
- Mostrar:
  - Alumno.
  - Examen.
  - Tipo de evento.
  - Severidad.
  - Fecha.
  - Motivo.

### Entregable

Sentinel recibe un evento real, lo persiste, calcula una alerta y la muestra en el frontend.

---

## Sprint 3 — Oversight mínimo viable

**Objetivo:** implementar el panel docente básico antes de añadir tiempo real.

### Backend

Crear el módulo `oversight` con:

- Entidad de bitácora.
- Entidad de índice de integridad.
- Persistencia PostgreSQL.
- Listener para:
  - `SafeguardBlockEvent`
  - `SentinelAlertEvent`
  - `EvaluatorStateEvent`
- Endpoints:

```text
GET /oversight/exams/{examId}/sessions
GET /oversight/attempts/{attemptId}/timeline
GET /oversight/attempts/{attemptId}/integrity-index
GET /oversight/exams/{examId}/summary
```

- Calcular un índice inicial de integridad.

Ejemplo sencillo:

```text
100
- 30 por cambio de pestaña/aplicación
- 15 por pérdida repetida de foco
- 25 por bloqueo de SafeGuard
- 10 por desconexión prolongada
```

Los valores deben quedar documentados y ser configurables posteriormente.

### Frontend

- Crear ruta `/oversight`.
- Crear panel con:
  - Lista de alumnos.
  - Estado del examen.
  - Índice de integridad.
  - Total de alertas.
  - Último evento.
- Implementar semáforo visual:
  - Verde.
  - Amarillo.
  - Rojo.

### Entregable

Un profesor puede abrir un examen y consultar el estado de sus alumnos y su índice de integridad.

---

## Sprint 4 — WebSockets y monitoreo en tiempo real

**Objetivo:** lograr la primera demostración real del panel en tiempo real.

### Backend

- Configurar WebSockets de Spring.
- Definir endpoint, por ejemplo:

```text
/ws/oversight
```

- Definir canales o destinos:

```text
/topic/exams/{examId}
/topic/attempts/{attemptId}
```

- Publicar eventos hacia Oversight cuando se reciba:
  - Una alerta.
  - Un bloqueo.
  - Un cambio de estado del examen.
  - Una desconexión.
- Añadir autorización para que un profesor solo vea sus exámenes.
- Manejar:
  - Reconexión.
  - Desconexión.
  - Mensajes duplicados.
  - Error de publicación.

### Frontend

- Añadir cliente WebSocket.
- Actualizar el panel sin recargar la página.
- Mostrar notificaciones de nuevas alertas.
- Implementar reconexión automática.
- Añadir historial de eventos recientes.

### Entregable

Durante un examen simulado, un evento generado por Sentinel aparece en Oversight sin actualizar manualmente la página.

---

## Sprint 5 — SafeGuard backend y frontend mínimo

**Objetivo:** implementar la versión mínima demostrable de SafeGuard.

No recomiendo intentar en este sprint todas las restricciones posibles. Primero deben funcionar las que sean técnicamente viables en el entorno objetivo.

### Backend

- Crear configuración de restricciones por examen.
- Crear eventos de bloqueo:

```text
SafeguardBlockEvent
```

- Crear endpoint:

```text
GET  /safeguard/exams/{examId}/config
POST /safeguard/events
```

- Registrar:
  - Tipo de restricción.
  - Alumno.
  - Intento.
  - Fecha.
  - Resultado.
- Publicar el evento hacia Oversight y EVALUATOR.

### Frontend

- Crear servicio `SafeguardService`.
- Implementar detección web de:
  - Pérdida de foco.
  - Cambio de visibilidad de la pestaña.
  - Uso de atajos básicos.
  - Copiar/pegar dentro de la aplicación.
- Mostrar aviso al alumno cuando ocurra una restricción.
- Enviar el evento al backend.

### Tauri

Si Tauri ya está configurado, implementar:

- Estructura inicial del proyecto.
- Comando de ejecución de escritorio.
- Comunicación Angular-Tauri.
- Una prueba mínima de acceso a una capacidad nativa.

Si Tauri aún no existe, se debe documentar como **limitación del MVP**. No conviene prometer bloqueo absoluto del sistema operativo en solo un sprint.

### Entregable

Un alumno inicia un examen, pierde el foco o utiliza una acción restringida, y el evento aparece en EVALUATOR y Oversight.

---

## Sprint 6 — Redis, autosave y resiliencia

**Objetivo:** hacer que el examen sea tolerante a desconexiones y concurrencia básica.

### EVALUATOR

- Crear sesión de examen en Redis.
- Guardar en Redis:
  - Estado del examen.
  - Tiempo restante.
  - Última respuesta.
  - Última actividad.
- Implementar autosave periódico.
- Recuperar una sesión después de:
  - Recargar la página.
  - Perder conexión brevemente.
  - Reiniciar el frontend.
- Cerrar automáticamente el examen cuando expire el temporizador.
- Evitar que una respuesta enviada dos veces cree inconsistencias.

### Backend

- Añadir idempotencia para guardar respuestas.
- Invalidar cachés al:
  - Guardar respuestas.
  - Finalizar intento.
  - Revisar respuestas abiertas.
- Probar concurrencia con varios alumnos.

### Frontend

- Mostrar:
  - Tiempo restante.
  - Último guardado.
  - Estado de conexión.
  - Aviso de recuperación.
- Añadir reintento de autosave.

### Entregable

Un alumno puede continuar un examen después de una desconexión breve sin perder las respuestas guardadas.

---

## Sprint 7 — Integración completa y pruebas

**Objetivo:** ejecutar el flujo completo de los cuatro módulos.

### Flujo obligatorio

```text
Alumno inicia examen
        |
        v
EVALUATOR publica estado
        |
        v
SafeGuard detecta restricción
        |
        v
SafeguardBlockEvent
        |
        +-------------------> Oversight registra evento
        |
        +-------------------> EVALUATOR actualiza estado
        |
        v
Sentinel genera alerta
        |
        v
SentinelAlertEvent
        |
        +-------------------> Oversight actualiza panel
        |
        +-------------------> EVALUATOR registra incidencia
        |
        v
Alumno finaliza examen
        |
        v
Oversight calcula índice final
```

### Pruebas

- Pruebas de integración entre los cuatro módulos.
- Prueba de examen completo.
- Eventos duplicados.
- Listener que falla.
- Reconexión WebSocket.
- Varias sesiones simultáneas.
- Expiración del temporizador.
- Acceso no autorizado a otro examen.
- Repetición de una petición de respuesta.
- Pérdida de conexión durante autosave.

### Entregable

Un script de prueba automatizado o manual que reproduzca el flujo completo y deje evidencia.

---

## Sprint 8 — Release candidate, despliegue y documentación

**Objetivo:** cerrar el proyecto como una versión entregable.

### Calidad

- Corregir bugs críticos.
- Ejecutar regresión completa.
- Ejecutar lint, tests y builds.
- Revisar logs.
- Verificar migraciones desde una base vacía.
- Revisar que no existan secretos en el repositorio.

### Deployment

- Crear configuración de producción.
- Añadir Nginx.
- Configurar:

```text
/       -> frontend
/api    -> backend
/ws     -> WebSocket
```

- Configurar HTTPS.
- Desplegar en:
  - Oracle Cloud,
  - VPS,
  - o servidor de CIDEM.
- Configurar dominio o subdominio.
- Crear backups de PostgreSQL.
- Documentar restauración.

### CI/CD

Mantener CI para:

- Tests backend.
- Tests frontend.
- Lint.
- Build de imágenes.

Añadir CD para:

- Construcción de imágenes.
- Publicación en un registry.
- Despliegue por SSH o mediante runner.
- Migraciones Flyway.
- Healthcheck posterior al despliegue.

### Documentación

Crear como mínimo:

```text
docs/
  architecture.md
  internal-events.md
  deployment.md
  testing-plan.md
  user-manual.md
  uat-plan.md
  known-limitations.md
```

### Entregable

Release candidate desplegado, probado y documentado.

Todos deben participar en:

- Revisión de arquitectura.
- Pruebas de integración.
- Corrección de bugs.
- UAT.
- Demo de cada viernes.
- Documentación técnica.

# Ceremonias semanales

## Lunes

- Sprint Planning.
- Confirmación de tareas.
- Revisión de contratos y dependencias.

## Martes a jueves

- Desarrollo.
- Integración continua.
- Pull requests pequeños.
- Pruebas locales.

## Viernes

- Demo funcional.
- QA.
- Revisión de incidencias.
- Actualización del tablero.
- Decisión de aceptación del sprint.

# Criterios de aceptación globales

El proyecto puede considerarse terminado cuando:

- [ ] El backend y frontend levantan con un solo comando.
- [ ] Existen los cuatro módulos dentro del backend único.
- [ ] EVALUATOR permite crear, realizar y finalizar un examen.
- [ ] Redis gestiona sesión, temporizador o autosave.
- [ ] Sentinel recibe y persiste eventos.
- [ ] SafeGuard detecta y reporta restricciones mínimas.
- [ ] Oversight muestra alertas y estados.
- [ ] Los eventos internos se publican y consumen correctamente.
- [ ] Oversight recibe actualizaciones vía WebSocket.
- [ ] El índice de integridad se calcula y persiste.
- [ ] Existe una prueba completa de integración.
- [ ] Se ejecutaron pruebas de carga básicas.
- [ ] Se ejecutaron pruebas de seguridad básicas.
- [ ] El sistema está desplegado en un entorno real.
- [ ] Hay documentación técnica y manual de usuario.
- [ ] El CI está en verde.
- [ ] No existen secretos productivos en el repositorio.

En ese escenario, no intentaría completar simultáneamente:

- Bloqueo total del sistema operativo.
- Android e iOS.
- Tauri completo.
- Grafana/Prometheus.
- RabbitMQ.
- Alta disponibilidad.
- Microservicios.
- Reportes avanzados.

El alcance mínimo realista para cerrar en dos meses es: **EVALUATOR funcional + Sentinel persistente + SafeGuard básico + Oversight con WebSockets + eventos internos + Docker + pruebas de integración + despliegue en una VM**.

## Pendientes de auditoría (Fase 2 — 2026-10-09)

Cuatro revisiones (seguridad, calidad, backend, frontend) con los agentes de `.opencode/agents/`. Todo lo crítico/corregible de esfuerzo razonable se arregló en esta fase; esto queda **documentado como pendiente a propósito**:

### Backend

- **N+1 en `GetExamResultUseCase`** (alto): por cada resultado se resuelven intento/examen/usuario/respuestas de forma individual. Requiere fetch-join o proyección SQL; no se tocó para no arriesgar la calificación en caliente.
- **Límites de módulo** (alto): `oversight` importa dominio/aplicación de `sentinel` (`AlertSeverity`, `AttemptId`, `AlertGeneratedEvent`, …) y los contratos entre módulos no pasan todos por `shared.events`. Unificar nombres de eventos implica cambiar publicadores y listeners en varios módulos a la vez.
- **El dominio depende de Spring** (alto): `BCryptPasswordHasher`, `IntegrityScoringPolicy`, `SafeguardRiskPolicy` y `AlertEscalationPolicy` son beans de Spring dentro de `domain`. Moverlos a adapters obliga a replantear la creación de los use-cases.
- **`@TransactionalEventListener(AFTER_COMMIT)`**: los listeners entre módulos corren dentro de la transacción del publicador; si el consumidor falla, revierte datos de otro módulo. Debe migrarse a `AFTER_COMMIT` con manejo de errores propio.
- **Carrera en el primer snapshot de oversight** (medio): si llegan dos eventos a la vez para un intento aún sin snapshot, ambos pasan el lock (la fila no existe), el segundo insert choca con `uq_oversight_snapshot_attempt` y la transacción entera (log + snapshot) se revierte, perdiendo ese evento. Corrección futura: reintentar al violar la unique con una transacción nueva (`REQUIRES_NEW`), porque dentro de la misma transacción ya está marcada para rollback.
- **El handler global de excepciones vive en `auth`** (medio): `GlobalExceptionHandler` importa excepciones de `evaluator`, y su `@ExceptionHandler(Exception.class)` convierte en 500 lo que Spring habría devuelto como 404/405/415/400/401 (`NoResourceFoundException`, `HttpRequestMethodNotSupportedException`, …). Moverlo a `shared_kernel` y estrechar el catch genérico.
- **`WebSocketConfig.setAllowedOriginPatterns("*")`** (`/ws/sentinel`): acepta cualquier origen; debe limitarse a `CORS_ALLOWED_ORIGINS` (el propio comentario del código lo pide).
- **Payloads de Sentinel sin atar a la sesión**: `attemptId`/`studentId` llegan en el mensaje y no se contrastan con el principal del JWT, así que un alumno podría registrar eventos de otro intento.
- **Refresh tokens en texto plano**: el valor crudo (`UUID.randomUUID()`) se guarda en `refresh_tokens`; además el frontend lo guarda en `localStorage`. Ideal: hash en BD + cookie `HttpOnly` + rotación.
- **`metadata` de SafeGuard es `String`** y devuelve 500 con entrada no-JSON (ya anotado en `safeguard/README.md` §8); los 4 tipos de enum sin regla de riesgo quedan igual.
- **`trust-proxy-header` apagado por defecto** en el rate limit: correcto hasta que haya un proxy de confianza; mientras, `X-Forwarded-For` se ignora a propósito.

### Frontend

- **`/exams/:id/take` es un callejón sin salida**: `ExamHttpRepository.submitAttempt` siempre lanza error y pasa `examId` como `attemptId`; la ruta que realmente toma exámenes es `exam-attempt`. Falta decidir la ruta canónica antes de borrarla.
- **SCSS todavía duplicado entre páginas**: `.dashboard-card*` se movió a `global.scss`, pero conviene revisar el resto clase por clase antes de mover (riesgo de romper el budget de 4 KB por página).
- **`console.error` en callbacks de error**: se dejaron a propósito para diagnóstico de red; si se quiere producción sin logs, condicionarlos por entorno.

### Producto / legal

- **Faltan política de privacidad, aviso legal y consentimiento informado**: la plataforma graba cámara/micrófono, captura pantalla y monitorea pestañas de estudiantes, y el repo no tiene ningún documento de ese tipo. Se evaluó el MCP `legal-docs-mcp` como atajo y **se descartó** (paquete discontinuado en npm, sin repo fuente, plantillas mercantiles francesas sin relación con proctoring — ver `docs/evaluacion-legal-docs-mcp.md`). Recomendación: redactarlos directamente en español (qué se captura, finalidad, retención, derechos ARCO) y registrar la aceptación con timestamp al iniciar el intento.

## SonarQube (Fase 2 — 2026-10-10)

Escaneo local con `sonarqube:community` + `sonarsource/sonar-scanner-cli` en podman (red aislada, volúmenes reutilizables; no toca el Postgres de la app). Guiones para repetirlo en `/tmp/opencode/sonar/`: `up.sh` levanta la pila y `scan.sh` escanea **y espera al Compute Engine** — sin esa espera el reporte se pierde y la API devuelve 0 issues. Alcance: `integrity-core/src/main/java` + `integrity-core-fr/src` (14.4 K líneas).

| | Antes | Después |
|---|---|---|
| Issues abiertas | 77 | **64** |
| BLOCKER | 1 | **0** |
| CRITICAL | 5 | **0** |
| Vulnerabilidades | 1 | **0** |
| Bugs | 7 | 4 |
| Deuda (sqale) | 383 min | 314 min |

### Cerrados

- `9277b84` — java:S1845 **BLOCKER** (constante `SOURCE` junto al componente `source` de `SafeguardBlockEvent` → `SOURCE_SAFEGUARD`), java:S3077 (`volatile` → `AtomicReference` en el listener), java:S1186 (comentario *dentro* del constructor vacío de JPA; el comentario encima no lo cierra).
- `8648615` — typescript:S7059 ×2 **CRITICAL** (async en constructor: `token-storage` pasó a getter perezoso, `evaluator-demo` pasó a `ngOnInit`), typescript:S6544 ×2 (`async ngOnInit` → `void` + método privado en home y app-header), typescript:S2245 (fallback `Math.random` justificado con comentario + `NOSONAR`).
- `9d98ad9` — java:S3776 ×2 **CRITICAL**: `GetExamResultUseCase.calculate` (19) y `ListOpenEndedReviewsUseCase.execute` (27) sobre el límite de 15. De paso se eliminó el doble `findByAttemptId` por intento y la lista `nonSubmittedAttempts` que nadie leía.
- `cf77266` + esta pasada — typescript:S1854 (asignación inútil, borrado con la reescritura de oversight), typescript:S1121, typescript:S3863 ×2 (imports duplicados), Web:S7926 (viewport que bloqueaba el zoom).

### Aceptados a propósito (falsos positivos)

- **`Web:MouseEventWithoutKeyboardEquivalentCheck` ×4** — son los 4 "bugs" que quedan. Las filas con `(click)` de ratón tienen su camino de teclado/AT en el `<button class="btn btn-link p-0 text-reset">` de la celda del nombre (patrón documentado en `DESIGN-SYSTEM.md` § Accesibilidad) y el `<ion-button>` del encabezado envuelve un `<button>` nativo. Quitar el clic de la fila empeoraría la UX del monitor sin ganar nada accesible.
- `typescript:S2245` — ver arriba (el id solo correlaciona eventos, no firma nada, y solo corre en contextos http inseguros).

### Pendientes (backlog sin tocar)

- `Web:S6819` ×20 MAJOR — `role="status"` donde Sonar pide `<output>`. Es una mejora real de accesibilidad, pero son 20 elementos en 8 páginas: hacerlo en una pasada dedicada con capturas.
- `java:S6213` ×4 MAJOR — `RecordLogEntryService.record(…)` usa el identificador restringido `record`. Renombrar toca puerto + listener + tests por un tema cosmético.
- `java:S107` ×3 MAJOR — constructores con 8 parámetros (`User`, …); quiere un DTO/record de construcción.
- `css:S125` ×3 + `java:S125` ×1 MAJOR — código comentado en `ionic.scss` y `RateLimitConfig`.
- `Web:S7927` ×2 (nombre accesible ≠ etiqueta visible), `css:S4666` (`:root` duplicado en `variables.scss`), `css:S4667` (`app.component.scss` vacío) y la cola de estilo: `S2933`, `S3626`, `S6582` ×3, `S7755`, `S7766`, `S7778`.
- `java:S1135` ×5 INFO — TODOs que **son** el roadmap (Sprint 1/7 de SafeGuard en evaluator, etc.); no es deuda.
- `java:S8688` ×4 INFO — `OffsetDateTime.now()` sin `ZoneId` explícito.
