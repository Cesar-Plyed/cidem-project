# Manual del Usuario — CIDEM Integrity

## Profesor

1. **Iniciar examen**: Ir a `/exams` → "Nuevo examen" → configurar reglas (max tab switches, timeout).
2. **Ver panel de Oversight**: Ir a `/oversight` → ver tabla de alumnos con semáforo de integridad.
3. **Consultar alertas**: Ir a `/alerts` → filtrar por alumno, tipo de alerta, severidad.

## Alumno

1. **Iniciar examen**: Ir a `/exams` → "Iniciar intento" → se abre la sesión con Tauri.
2. **Perder foco**: Cambiar de pestaña del navegador → toast notification + badge rojo.
3. **Copiar/Pegar**: Usar Ctrl+C / Ctrl+V dentro del editor → toast con conteo.
4. **Finalizar**: Botón "Finalizar examen" en la barra superior.

## Capturas de UI

| Imagen | Descripción |
|--------|-------------|
| `[image: exam-start.png]` | Pantalla de inicio de intento de examen |
| `[image: oversight-panel.png]` | Panel de oversight con semáforo |
| `[image: alerts-panel.png]` | Panel de alertas del profesor |
| `[image: toast-notification.png]` | Toast de restricción con badge rojo |
