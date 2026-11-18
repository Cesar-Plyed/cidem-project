# Política de seguridad

## Versiones soportadas

CIDEM Integrity está en fase **alpha**. Solo la rama `main` recibe correcciones
de seguridad; las ramas `feat/*` y los tags `*-alpha.*` no tienen soporte.

| Versión | Soporte |
|---|---|
| `main` (alpha) | Sí |
| Tags `vX.Y.Z-alpha.N` | No (solo referencia histórica) |

## Cómo reportar una vulnerabilidad

**No abras un issue público.** Usa el reporte privado de vulnerabilidades de
GitHub (*Security* → *Report a vulnerability*) e incluye:

- Descripción del fallo y su impacto.
- Pasos para reproducirlo (versión, navegador, rol de la cuenta).
- Prueba de concepto, si la tienes.
- Si lo deseas, tu nombre para acreditar el hallazgo.

Acuse de recibo en un máximo de 5 días laborables y una primera evaluación en
10 días. Si el fallo es crítico, se publicará un aviso y un parche lo antes
posible.

## Alcance

Se consideran dentro del alcance:

- Autenticación, gestión de sesiones y JWT (`integrity-core/.../auth/`).
- Control de acceso por roles y endpoints de exámenes.
- Tratamiento de datos personales y módulo legal.
- Controles de integridad del módulo SafeGuard y del escritorio Tauri.

Fuera del alcance: ataques de denegación de servicio, ingeniería social,
vulnerabilidades en dependencias sin impacto demostrable y entornos de demo con
datos de prueba.

## Buenas prácticas para desplegar

- Nunca reutilices los valores de `.env.example`; genera secretos reales.
- `JWT_SECRET` debe tener al menos 32 caracteres aleatorios.
- Sirve la aplicación únicamente por HTTPS.
- Restringe `CORS_ALLOWED_ORIGINS` a los dominios reales.
- Ejecuta `docker compose` con un usuario sin privilegios y mantén las imágenes
  actualizadas.
