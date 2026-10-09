# Guía de estilo — Integrity (Ocean Depths + Bootstrap 5)

Guía para (re)escribir páginas de `src/app/presentation/pages/`. Objetivo visual:
**educativo-moderno**, paleta Ocean Depths, Bootstrap 5 para layout/componentes y
Ionic solo como shell (`ion-content`).

## Capas de CSS (orden en `angular.json → styles`)

1. `@fontsource` (Source Sans 3 = interfaz, Fraunces = títulos)
2. `src/theme/ionic.scss` — CSS base de Ionic
3. `src/theme/bootstrap.scss` — Bootstrap 5 compilado con la paleta
4. `src/theme/variables.scss` — tokens `--ion-color-*` / `--ocean-*`
5. `src/global.scss` — capa final: clases compartidas (`.page`, `.card`, `.btn`,
   `.form-*`, `.field-error`, `.loading`, `.auth__*`, `.page-heading`)

**Regla:** estilos compartidos van en `global.scss`; el SCSS de la página solo
agrega lo propio de esa página y debe mantenerse < 2 KB (budget: error a 4 KB).

## Patrón de página

```html
<app-header title="Título de la página" backHref="/ruta/anterior" />

<ion-content [fullscreen]="true">
  <div class="page ion-padding">
    <header class="page-heading">
      <p class="eyebrow">Contexto corto</p>
      <h2>Título principal</h2>
      <p>Subtítulo opcional.</p>
    </header>
    <!-- contenido con grid de Bootstrap -->
  </div>
</ion-content>
```

- `app-header` (`presentation/components/app-header/`) ya incluye marca, botón
  atrás (si `backHref` no es null), rol y "Cerrar sesión". No dupliques toolbars.
- Contenedor: `.page` (max 1100px) o `.page-narrow` (720px).
- Grid Bootstrap: `row g-3` + `col-12 col-md-6 col-xl-4` para tarjetas.
- Tarjetas: `.card > .card-body`, título en `h3`, texto auxiliar en
  `.dashboard-card__hint` o párrafo normal.
- Botones: `.btn btn-primary` (navy) / `.btn btn-outline-secondary` (teal) /
  `.btn btn-danger` (destructivo). Tamaños: `btn-lg` para CTA principal.
- Formularios: `.mb-3` + `label.form-label` + `input/select.form-control(-select)`;
  errores con `.field-error` justo debajo del control (ya con `aria-describedby`
  cuando aporte).
- Alertas: `.alert alert-danger|alert-success` con `role="alert"`.
- Carga: `<div class="loading"><span class="spinner-border" role="status"></span><span class="ms-2">…</span></div>`
- Estados vacíos: párrafo con texto claro en español.
- Badges de estado: `span.badge rounded-pill text-bg-*` (success/warning/danger/secondary).

## TS de la página

- Imports: quitar todo `IonX` que ya no se use en el template. `ion-content`
  sigue siendo Ionic (`IonContent`); todo lo demás suele ser HTML nativo + Bootstrap.
- Agregar `AppHeaderComponent` a `imports`.
- **No tocar la lógica** (use-cases, signals, formularios): solo presentación.
- Comentarios en el TS en español (convención del repo); identifiers en inglés.

## Accesibilidad

- Un `h2` como título de página (el `h1` está implícito en el heading visual);
  `h3` dentro de tarjetas.
- Todo control con `<label for>`; iconos con `aria-label`; botones de icono con
  `aria-label` descriptivo en español.
- Estados de carga con `role="status"`.

## Colores (ya resueltos en tokens; no re-declarar)

- Navy `#1a2332` = acción principal/fondo toolbar; teal `#2d8b8b` = acento;
  seafoam `#a8dadc` = superficie suave; cream `#f1faee` = fondo.
- Texto secundario: `#4a5768`; enlaces: `--ocean-teal-ink` (`#1f6f6f`).
