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
