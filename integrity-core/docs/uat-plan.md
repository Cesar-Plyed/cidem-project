# UAT Plan — CIDEM Integrity

## Casos de prueba

| # | Caso | Pre-condición | Acción | Resultado esperado |
|---|------|---------------|--------|--------------------|
| 1 | Profesor consulta panel | Examen activo | Abrir `/oversight` | Ver tabla de alumnos con semáforo |
| 2 | Alumno pierde foco | Examen activo | Cambiar pestaña del navegador | Toast notification + badge rojo |
| 3 | Alumno copia texto | Examen activo | Ctrl+C en editor | Toast con conteo "Copiado (1 veces)" |
| 4 | Alumno finaliza examen | Examen activo | Click "Finalizar" | Estado cambia a COMPLETED |
| 5 | Profesor crea nuevo examen | Sin exámenes | Click "Nuevo examen" + guardar | Examen aparece en lista |
| 6 | Alumno abre Tauri | Tauri instalado | Click "Abrir en Tauri" | App nativa se abre con la sesión |
| 7 | Sesión expira | Timeout configurado | Esperar maxDuration | Estado → EXPIRED, toast al alumno |

## Criterios de aprobación

- Todos los casos 1-7 pasan sin error.
- Semáforo de integridad se actualiza en tiempo real (< 2s de latencia).
- Toast notifications auto-dismiss en ≤ 5s.
- Badge rojo muestra conteo correcto.
