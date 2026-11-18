# Contribuir a CIDEM Integrity

Gracias por tu interés. Este proyecto nació como proyecto de estadías
profesionales y sigue en fase alpha, así que las contribuciones pequeñas y bien
acotadas son las más fáciles de revisar.

## Antes de empezar

- Abre un issue describiendo el cambio o el fallo antes de un PR grande.
- Para fallos de seguridad, sigue [`SECURITY.md`](./SECURITY.md) y no uses issues públicos.
- Respeta el [`CODE_OF_CONDUCT.md`](./CODE_OF_CONDUCT.md).

## Entorno

Requisitos: Java 25, Node.js, Docker o Podman con Compose.

```bash
cp .env.example .env          # rellena los valores locales
docker compose up -d db redis
cd integrity-core && ./mvnw spring-boot:run
cd integrity-core-fr && npm install && npm start
```

## Comprobaciones obligatorias antes de un PR

```bash
cd integrity-core && ./mvnw test
cd integrity-core-fr && npm run lint && npm test
cd integrity-core-fr && npm run build -- --configuration development
```

Si tocas la base de datos, añade una migración nueva en
`integrity-core/src/main/resources/db/migration/`; nunca edites una ya aplicada.

## Estilo

- **Java:** MapStruct para DTO↔entidad, tests `*Test.java`.
- **TypeScript:** app zoneless; usa `signal` y `.set()` en callbacks, no mutaciones
  dentro de `subscribe()`.
- **SCSS por página:** máximo 2 KB; los estilos compartidos van en `src/global.scss`.
- **Commits:** conventional commits (`feat(scope): ...`, `fix(scope): ...`,
  `docs(scope): ...`, `chore(scope): ...`).
- **Idioma:** código y comentarios en inglés; documentación de usuario en español.

## Pull requests

1. Crea una rama desde `main`: `feat/mi-cambio` o `fix/mi-arreglo`.
2. Un PR = un cambio coherente. Incluye tests cuando toques lógica.
3. Describe qué problema resuelve y cómo lo probaste.
4. El PR debe pasar CI (build, tests, lint y smoke).

## Licencia de las contribuciones

Al enviar un PR aceptas que tu contribución se distribuya bajo la licencia MIT
del proyecto.
