# Evaluación: legal-docs-mcp

*Informe de 2026-10-09. Investigación solo lectura (webfetch + registro de npm + auditoría del bundle publicado en unpkg); no se instaló ni ejecutó el paquete.*

## Qué es

- **Fuente canónica**: paquete npm **`legal-docs-mcp`** (<https://registry.npmjs.org/legal-docs-mcp>). No existe en PyPI (404 verificado en <https://pypi.org/pypi/legal-docs-mcp/json>).
- **Última versión**: 2.1.0, publicada 2026-04-12 (8 versiones entre 2026-03-25 y 2026-04-12). Autoría en el registro: "Thomas Gorisse" → "SceneView Tools" → "MCP Tools Lab"; mantenedor npm `thomasgorisse`. Enlaces de financiación: `github.com/sponsors/sceneview` y `polar.sh/sceneview`.
- **MUY IMPORTANTE — paquete discontinuado**: TODAS las versiones, incluida la última (2.1.0), están marcadas como *deprecated* en el registro: "This package is discontinued and no longer maintained…". El campo `modified` del registro es 2026-08-22 (la desactivación se aplicó hacia esa fecha).
- **El repositorio declarado no existe**: `package.json` apunta a <https://github.com/mcp-tools-lab/legal-docs-mcp> y la organización <https://github.com/mcp-tools-lab>, ambos **HTTP 404** (verificado). Solo queda el código compilado publicado en npm. Su PRIVACY.md además cita un dominio de contacto (`contact@legal-docs-mcp.dev`) que no pude verificar.
- Homónimo distinto: existe también `@ebenova/legal-docs-mcp` / `ebenova-legal-docs-mcp` ("Ebenova Legal Docs", API remota con jurisdicciones africanas/anglofonas, según <https://mcp.so/servers/legal-docs-mcp>). **No es el mismo proyecto**; el más probable por nombre exacto es el de npm.

## Qué ofrece

Servidor MCP **solo stdio** (Node ≥ 18, binario `dist/index.js`, arranque típico `npx -y legal-docs-mcp`). Audité el código publicado de 2.1.0 vía unpkg (<https://unpkg.com/legal-docs-mcp@2.1.0/dist/index.js>, `/dist/usage.js`, `/dist/tools.js`, `/dist/templates/index.js`, `/dist/types.js`, listado completo en <https://unpkg.com/legal-docs-mcp@2.1.0/?meta>):

- **13 herramientas, todas de relleno de plantillas** (texto hardcodeado en JS): `generate_cgv`, `generate_cgu`, `generate_privacy_policy`, `generate_nda`, `generate_freelance_contract`, `generate_mentions_legales`, `generate_devis`, `generate_terms_of_service`, `generate_rgpd_policy`, `generate_cession_contrat`, `generate_en_privacy_policy`, `generate_en_tos`, `generate_dmca_notice`.
- Es decir: plantillas de **mercantil/francesas para freelancers y pymes** (CGV/CGU/contratos/devis) + 2-3 plantillas genéricas de privacidad (RGPD francófono y una "en") + DMCA (EE. UU.). **No consulta ninguna base de datos jurídica, no valida compliance, no revisa documentos existentes.**
- Sin recursos ni prompts MCP relevantes; sin dependencias más allá de `@modelcontextprotocol/sdk`.

## Requisitos y coste

- **Sin claves de API y sin servicios de pago obligatorios.** El "límite freemium" (5 docs/mes, Pro 19,99 €/mes) es **puro adorno**: el contador vive en un `Map` en memoria (`usage.js`, commentario literal: *"In production, this would be backed by a database"*), el flag `_isPro` es un argumento que el propio cliente envía (se elude pasando `_isPro: true`), y no hay pasarela de pago ni verificación. El README de npm además anuncia productos "Pro" distintos (9,99 €/mes) — incoherencia típica de plantilla de marketing.
- Dato de uso: **36 descargas/mes** (últimos 30 días, <https://api.npmjs.org/downloads/point/last-month/legal-docs-mcp>).

## Riesgos (seguridad/privacidad/licencia)

- **Cadena de suministro**: sin repositorio fuente, sin maintainer activo, descontinuado y con ~36 descargas/mes. Un `npx -y` a futuro ejecuta bytes que ya nadie mantiene; ante un compromiso de la cuenta npm nadie publicaría un fix. Si alguien lo usara, sería con versión pinneada y tras auditar.
- **Telemetría/red: no encontré ninguna.** Escaneé el `dist` publicado completo por `fs`, `http(s)`, `child_process`, `fetch`, `axios`, `net`, `dns`, `process.env`, `readFile/writeFile`, `os.homedir`: **cero coincidencias**. Sus PRIVACY.md/TERMS.md confirman procesamiento solo en memoria y sin envíos externos. (Salvedad: leí el código, no lo ejecuté.)
- **Licencia**: MIT (Copyright 2026 Thomas Gorisse, verificado en el paquete). Uso académico/comercial permitido. Sus propios TERMS.md declaran que los documentos son plantillas informativas, **no asesoramiento legal**, y someten el servicio a **ley francesa / tribunales de París**.
- **Riesgo jurídico real**: el mayor peligro no es técnico sino de **falsa confianza** — publicar una política de privacidad genérica generada por una plantilla desconocida, sin revisión, en un sistema que graba cámara/micropantalla de estudiantes.

## Aplicabilidad a Integrity

**Prácticamente nula.** Integrity es una plataforma de examen con vigilancia (cámara, micrófono, captura de pantalla, monitoring de pestañas vía Tauri, índice de integridad): su superficie legal real es protección de datos de estudiantes, consentimiento informado, retención de grabaciones, finalidad y base jurídica del tratamiento, y normativa académica. Este paquete ofrece:

- CGV, CGU, devis, NDA, contrato de freelance, cesión de derechos: **irrelevantes** para un proyecto universitario.
- Su mejor plantilla (`generate_rgpd_policy`) es una **política de privacidad genérica de web SaaS en francés**, orientada a comercio electrónico francés — no trata vídeo/audio de estudiantes, datos académicos, menores, retención ni responsable/encargado del tratamiento. El proyecto escribe su documentación en **español** y la jurisdicción aplicable (España/LATAM) no es Francia.
- No hay en el repo ni EULA ni política de privacidad ni términos, así que "generador de plantillas" *suena* útil, pero el modelo con el que trabajas puede redactar directamente algo igual de bueno y **mucho más adaptado** sin instalar nada. El MCP no aporta datos legales verificables que el LLM no tenga.

## Veredicto

**No instalar.** Razones, en orden: (1) está **descontinuado y sin repositorio fuente** — mantenimiento cero y riesgo de suministro injustificable; (2) su contenido es **plantilla mercantil francesa** sin relación con privacidad/proctoring; (3) su único valor (rellenar plantillas genéricas) lo hace el propio agente sin herramientas; (4) el freemium "Pro" es marketing vacío. Aunque la revisión original pidiera no instalar sin permiso, esta evaluación confirma que **tampoco merece la pena pedir ese permiso**.

## Alternativas

1. **Redactar directamente** (con el agente, en español): política de privacidad, aviso legal, documento de consentimiento informado del examen (cámara/micrófono/pantalla, finalidad, retención, derechos ARCO bajo LGPD/RGPD aplicable) y condiciones de uso. Es una tarea de una tarde y queda versionada en el repo.
2. Si se quiere apoyo jurídico-grounded en español: echar un ojo a **`spanish-law-mcp`** (Ansvar-Systems, <https://github.com/Ansvar-Systems/spanish-law-mcp> — texto de LOPDGDD/RGPD/ENS/LSSI; solo visto en búsqueda, **no auditado**, y conviene verificar mantenimiento antes de usarlo), o guías oficiales de la AEPD para centros educativos.
3. Para el flujo de consentimiento en la app: implementarlo en Angular/Ionic junto al arranque del examen (checkbox + registro de aceptación en la DB con timestamp), no con plugins externos.

## Fuentes consultadas

- <https://registry.npmjs.org/legal-docs-mcp> (metadatos, versiones, deprecación, README)
- <https://unpkg.com/legal-docs-mcp@2.1.0/dist/index.js>, `/dist/usage.js`, `/dist/tools.js`, `/dist/templates/index.js`, `/dist/types.js`, `/?meta`, `/PRIVACY.md`, `/TERMS.md`, `/LICENSE`
- <https://api.npmjs.org/downloads/point/last-month/legal-docs-mcp>
- <https://github.com/mcp-tools-lab/legal-docs-mcp> y <https://github.com/mcp-tools-lab> (ambos 404)
- <https://pypi.org/pypi/legal-docs-mcp/json> (404)
- <https://mcp.so/servers/legal-docs-mcp> (homónimo Ebenova)

**No verificado**: existencia/actividad real del autor tras el cierre del repo, calidad jurídica real de las plantillas (no generé documentos), y si el dominio de contacto existe.
