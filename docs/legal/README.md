# Documentos legales

Textos legales de CIDEM Integrity, publicados también dentro de la aplicación en
rutas públicas (accesibles sin iniciar sesión):

| Documento | Ruta | Archivo |
|---|---|---|
| Aviso legal (LSSI-CE art. 10) | `/legal/aviso-legal` | [`aviso-legal.md`](./aviso-legal.md) |
| Política de privacidad (RGPD/LOPDGDD) | `/legal/privacidad` | [`privacidad.md`](./privacidad.md) |
| Política de cookies | `/legal/cookies` | [`cookies.md`](./cookies.md) |
| Términos de uso | `/legal/terminos` | [`terminos.md`](./terminos.md) |

## Mantenimiento

El contenido que renderiza la aplicación vive en
`integrity-core-fr/src/app/core/services/legal-content.service.ts`. Si modificas
un documento aquí, actualiza el texto equivalente allí y la fecha `updatedAt` de
ambos.

## Antes de publicar en producción

Sustituye los datos marcados como pendientes por el titular:

- Domicilio social de CIDEMags S.L.
- Correo del DPO, si cambia (`dpo@cidemags.edu`).
- Jurisdicción aplicable, si el centro no está en España.

Los textos son una base razonable para un proyecto académico, pero no sustituyen
la revisión de un asesor legal antes de un despliegue real con datos de menores.
