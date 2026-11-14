#!/usr/bin/env bash
# =====================================================================
# smoke.sh — pruebas de humo contra el stack ya levantado
# Uso: ./scripts/smoke.sh
# Salida: código 0 si todo pasa; lista de fallos y código 1 si no.
# =====================================================================
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
API="${API_URL:-http://localhost:8080}"
WEB="${WEB_URL:-http://localhost:8081}"
DB_CONTAINER="${DB_CONTAINER:-integrity-db}"
PGUSER="$(grep -E '^POSTGRES_USER=' .env 2>/dev/null | cut -d= -f2)"
PGDB="$(grep -E '^POSTGRES_DB=' .env 2>/dev/null | cut -d= -f2)"
PGUSER="${PGUSER:-postgres}"
PGDB="${PGDB:-integrity_db}"

PASS=0; FAIL=0; FAILS=()

check() { # nombre, esperado, obtenido
  if [[ "$2" == "$3" ]]; then
    printf '  \033[1;32mPASS\033[0m %s\n' "$1"
    PASS=$((PASS + 1))
  else
    printf '  \033[1;31mFAIL\033[0m %s (esperado %s, obtenido %s)\n' "$1" "$2" "$3"
    FAIL=$((FAIL + 1)); FAILS+=("$1")
  fi
}

code() { curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$@" || echo "000"; }

echo "== Backend / infraestructura =="
check "health del backend"        "200" "$(code "$API/actuator/health")"
check "health UP"                 "sí"  "$(curl -s --max-time 5 "$API/actuator/health" | grep -q '"status":"UP"' && echo sí || echo no)"
check "OpenAPI disponible"        "200" "$(code "$API/v3/api-docs")"
check "Swagger UI disponible"     "200" "$(code "$API/swagger-ui/index.html")"

echo "== Frontend =="
check "frontend sirve SPA"        "200" "$(code "$WEB/")"

echo "== Autenticación =="
check "GET /auth/countries"       "200" "$(code "$API/auth/countries")"
check "registro inválido → 400"   "400" "$(code -X POST -H 'Content-Type: application/json' -d '{}' "$API/auth/register")"
check "login sin body → 400"      "400" "$(code -X POST -H 'Content-Type: application/json' -d '{}' "$API/auth/login")"

echo "== Legal (debe exigir auth) =="
check "consent/history sin token  → 401/403" "sí" \
  "$([[ "$(code "$API/api/legal/consent/history")" =~ ^40[13]$ ]] && echo sí || echo no)"
check "portability sin token      → 401/403" "sí" \
  "$([[ "$(code "$API/api/legal/portability/00000000-0000-0000-0000-000000000000/json")" =~ ^40[13]$ ]] && echo sí || echo no)"

echo "== Base de datos / Flyway =="
if docker inspect "$DB_CONTAINER" >/dev/null 2>&1; then
  check "contenedor db corriendo" "running" \
    "$(docker inspect -f '{{.State.Status}}' "$DB_CONTAINER")"
  check "migración V18 aplicada" "18" \
    "$(docker exec "$DB_CONTAINER" psql -U "$PGUSER" -d "$PGDB" -tAc \
      "select version from flyway_schema_history where success order by installed_rank desc limit 1" 2>/dev/null | tr -d ' ')"
  check "tabla consent_logs existe" "consent_logs" \
    "$(docker exec "$DB_CONTAINER" psql -U "$PGUSER" -d "$PGDB" -tAc \
      "select to_regclass('public.consent_logs')" 2>/dev/null | tr -d ' ')"
  check "regla append-only en consent_logs" "sí" \
    "$(docker exec "$DB_CONTAINER" psql -U "$PGUSER" -d "$PGDB" -tAc \
      "select count(*) from pg_rules where tablename='consent_logs'" 2>/dev/null | tr -d ' ' | grep -qv '^0$' && echo sí || echo no)"
else
  printf '  \033[1;33mSKIP\033[0m contenedor %s no encontrado\n' "$DB_CONTAINER"
fi

echo
if ((FAIL == 0)); then
  printf '\033[1;32mSmoke OK\033[0m — %d comprobaciones\n' "$PASS"
  exit 0
fi
printf '\033[1;31mSmoke FAIL\033[0m — %d ok, %d fallos: %s\n' "$PASS" "$FAIL" "${FAILS[*]}"
exit 1
