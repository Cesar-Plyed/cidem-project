# Testing Plan — CIDEM Integrity

## Cobertura mínima

| Componente | Cobertura objetivo |
|------------|-------------------|
| Backend (Java) | ≥ 80% |
| Frontend (TypeScript) | ≥ 70% |

## Estrategia de pruebas

| Tipo | Herramienta | Alcance | Frecuencia |
|------|-------------|---------|------------|
| Unit tests (Backend) | JUnit 5 + Mockito | Services, calculators, validators | Cada PR |
| Unit tests (Frontend) | Vitest + jsdom | Services, components | Cada PR |
| Integration tests | Testcontainers (Postgres, Redis) | Repositories, full app context | Cada push a main |
| E2E tests | Playwright | Flujo completo alumno → profesor | Sprint 8 |

## Cronograma

| Sprint | Actividad |
|--------|-----------|
| Sprint 7 | Unit tests backend (≥ 80%), frontend lint + Vitest (≥ 70%) |
| Sprint 8 | E2E Playwright + integration tests |

## Reglas

- Cada PR debe pasar lint + unit tests + build Docker.
- Integration tests requieren `docker compose up -d db redis` (Testcontainers).
- E2E tests se ejecutan en un runner con navegador (Playwright).
