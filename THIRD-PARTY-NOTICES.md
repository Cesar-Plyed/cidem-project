# Avisos de terceros

CIDEM Integrity se distribuye bajo la licencia MIT (ver [`LICENSE`](./LICENSE)).
Incluye o enlaza las siguientes dependencias de terceros, cuyas licencias se
respetan y se enumeran aquí.

La lista completa y sus textos están en `integrity-core-fr/package-lock.json`,
`integrity-core-fr/src-tauri/Cargo.lock` y en el árbol de Maven
(`mvn dependency:tree`).

## Frontend (npm)

| Dependencia | Licencia |
|---|---|
| `@angular/*` | MIT |
| `@ionic/angular`, `@ionic/storage-angular`, `ionicons` | MIT |
| `bootstrap` | MIT |
| `rxjs` | Apache-2.0 |
| `tslib` | 0BSD |
| `jwt-decode` | MIT |
| `@stomp/stompjs` | Apache-2.0 |
| `@tauri-apps/api`, `@tauri-apps/cli` | MIT OR Apache-2.0 |
| `@fontsource/fraunces`, `@fontsource/source-sans-3` | SIL Open Font License 1.1 |

## Escritorio (Rust / Tauri)

| Dependencia | Licencia |
|---|---|
| `tauri` y crates del ecosistema | MIT OR Apache-2.0 |
| Resto de crates | ver `Cargo.lock` y el campo `license` de cada crate |

## Backend (Maven)

| Dependencia | Licencia |
|---|---|
| Spring Boot y starters | Apache-2.0 |
| Springdoc OpenAPI | Apache-2.0 |
| MapStruct | Apache-2.0 |
| JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | Apache-2.0 |
| Flyway | Apache-2.0 |
| Micrometer | Apache-2.0 |
| PostgreSQL JDBC Driver | BSD-2-Clause |
| AssertJ, Testcontainers (pruebas) | Apache-2.0 / MIT |

## Notas

- **Apache-2.0:** al distribuir binarios (por ejemplo, instaladores Tauri) deben
  conservarse los avisos de copyright y el texto de la licencia de las
  dependencias afectadas.
- **SIL OFL 1.1:** las tipografías pueden redistribuirse con la aplicación, pero
  debe incluirse su licencia y no pueden venderse por separado.
- **Marca:** "CIDEM", el logotipo y los textos legales no están cubiertos por la
  licencia MIT del código.
