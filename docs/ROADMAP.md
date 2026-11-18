# Roadmap

CIDEM Integrity está en fase **alpha**. Este documento resume el estado del
proyecto y el trabajo previsto; no es un compromiso de fechas.

## Estado actual

Completado y en `main`:

- **Auth:** registro por rol, login JWT, rotación de refresh tokens, rate limit.
- **EVALUATOR:** banco de preguntas, exámenes, intentos, respuestas y resultados.
- **Sentinel:** ingesta de eventos, ventanas en Redis y generación de alertas.
- **Oversight:** panel en tiempo real por WebSocket y auditoría de acciones.
- **SafeGuard:** restricciones por examen, sesiones de vigilancia y puntuación de riesgo.
- **Legal:** consentimiento, derechos RGPD, retención, EIPD, brechas y auditoría.
- **Operación:** Docker Compose, Nginx, backups de PostgreSQL, CI y releases alpha de Tauri.

## Corto plazo — calidad y documentación

- [ ] Ejecutar los tests de integración con Testcontainers en CI (servicio Docker).
- [ ] Documentar la arquitectura y el despliegue en `docs/` (`architecture.md`, `deployment.md`).
- [ ] Manual de usuario para profesorado y alumnado.
- [ ] Terminar la limpieza de SCSS duplicado entre páginas.
- [ ] Eliminar la ruta muerta `/exams/:id/take` y dejar `exam-attempt` como flujo canónico.
- [ ] Mejorar la accesibilidad pendiente del informe de SonarQube (`role="status"` → `<output>`).
- [ ] Añadir capturas y guía rápida al README.

## Medio plazo — arquitectura

- [ ] Resolver el N+1 de `GetExamResultUseCase` con fetch-join o proyección SQL.
- [ ] Unificar los contratos entre módulos en `shared.events`.
- [ ] Sacar de `domain` los beans de Spring (`BCryptPasswordHasher`, políticas de riesgo).
- [ ] Mover los listeners entre módulos a `AFTER_COMMIT` con manejo de errores propio.
- [ ] Trasladar `GlobalExceptionHandler` a un módulo compartido y estrechar el catch genérico.
- [ ] Revisar los límites entre módulos (`oversight` ↔ `sentinel`).

## Largo plazo — producto

- [ ] Endurecer el ciclo de sesión y los canales en tiempo real.
- [ ] Evaluar attestation del cliente de escritorio para el bloqueo de navegador.
- [ ] Panel de resultados con exportación para el centro.
- [ ] Internacionalización (español/inglés).

## Fuera de alcance por ahora

Bloqueo total del sistema operativo, apps Android/iOS, Tauri completo,
Grafana/Prometheus, RabbitMQ, alta disponibilidad, microservicios y reportes
avanzados.

## Seguridad

Los hallazgos de seguridad abiertos no se publican en este documento; se
gestionan de forma privada según [`SECURITY.md`](../SECURITY.md).
