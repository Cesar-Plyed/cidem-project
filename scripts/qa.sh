#!/usr/bin/env bash
# =====================================================================
# qa.sh — suite de calidad completa con reporte en qa-artifacts/<fecha>/
#
# Uso:
#   ./scripts/qa.sh                 # tests backend + frontend, lint, build, smoke, E2E
#   ./scripts/qa.sh --up            # además levanta el stack si no responde
#   ./scripts/qa.sh --triage        # al terminar, abre issues en GitHub con los fallos
#   ./scripts/qa.sh --fast          # se salta tests backend pesados de integración
#
# Sale con código != 0 si algo falla. Pensado para CI nocturno y para uso local.
# =====================================================================
set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
ROOT="$(pwd)"

# Tests de Spring necesitan los placeholders (JWT_SECRET, códigos de registro).
# En CI vienen del workflow; en local, del .env de la raíz.
if [[ -f "$ROOT/.env" ]]; then
  set -a; # shellcheck disable=SC1091
  . "$ROOT/.env"; set +a
fi
STAMP="$(date +%Y-%m-%d_%H%M%S)"
ARTIFACTS="$ROOT/qa-artifacts/$STAMP"
mkdir -p "$ARTIFACTS/logs"

UP=0; TRIAGE=0; FAST=0
for arg in "$@"; do
  case "$arg" in
    --up) UP=1 ;;
    --triage) TRIAGE=1 ;;
    --fast) FAST=1 ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "Argumento desconocido: $arg" >&2; exit 2 ;;
  esac
done

info() { printf '\033[1;34m[qa]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[ ok ]\033[0m %s\n' "$*"; }
bad()  { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*"; }

declare -a NAMES=() STATUSES=() DETAILS=()

record() { # nombre, estado, detalle
  NAMES+=("$1"); STATUSES+=("$2"); DETAILS+=("$3")
}

run_step() { # nombre, log, comando...
  local name="$1" log="$2"; shift 2
  info "→ $name"
  local start; start=$(date +%s)
  if "$@" > "$log" 2>&1; then
    local dur=$(( $(date +%s) - start ))
    ok "$name (${dur}s)"
    record "$name" "PASS" "${dur}s"
    return 0
  fi
  local dur=$(( $(date +%s) - start ))
  bad "$name (${dur}s) — ver $log"
  record "$name" "FAIL" "${dur}s — ver $(basename "$log")"
  return 1
}

compose() {
  if docker compose version >/dev/null 2>&1; then docker compose "$@"; else docker-compose "$@"; fi
}

# ---------------------------------------------------------------------
# 1. Stack (opcional)
# ---------------------------------------------------------------------
if ((UP)); then
  info "levantando el stack…"
  if ./scripts/dev-up.sh > "$ARTIFACTS/logs/stack-up.log" 2>&1; then
    record "stack arriba" "PASS" ""
  else
    record "stack arriba" "FAIL" "ver logs/stack-up.log"
    bad "no se pudo levantar el stack"
  fi
fi

# ---------------------------------------------------------------------
# 2. Backend
# ---------------------------------------------------------------------
if ((FAST)); then
  run_step "backend: tests" "$ARTIFACTS/logs/backend-tests.log" \
    bash -c 'cd integrity-core && ./mvnw -B -q test -Dtest="!*IT" -DfailIfNoTests=false' || true
else
  run_step "backend: tests" "$ARTIFACTS/logs/backend-tests.log" \
    bash -c 'cd integrity-core && ./mvnw -B test' || true
fi

if [[ -d integrity-core/target/surefire-reports ]]; then
  mkdir -p "$ARTIFACTS/junit/backend"
  cp integrity-core/target/surefire-reports/*.xml "$ARTIFACTS/junit/backend/" 2>/dev/null || true
fi

# ---------------------------------------------------------------------
# 3. Frontend
# ---------------------------------------------------------------------
run_step "frontend: tests unitarios" "$ARTIFACTS/logs/frontend-tests.log" \
  bash -c 'cd integrity-core-fr && npx ng test --configuration ci' || true

run_step "frontend: lint" "$ARTIFACTS/logs/frontend-lint.log" \
  bash -c 'cd integrity-core-fr && npm run lint' || true

run_step "frontend: build" "$ARTIFACTS/logs/frontend-build.log" \
  bash -c 'cd integrity-core-fr && npm run build -- --configuration development' || true

# ---------------------------------------------------------------------
# 4. Smoke + E2E contra el stack
# ---------------------------------------------------------------------
if curl -fsS --max-time 3 http://localhost:8080/actuator/health >/dev/null 2>&1; then
  run_step "smoke API" "$ARTIFACTS/logs/smoke.log" ./scripts/smoke.sh || true

  run_step "E2E (Playwright)" "$ARTIFACTS/logs/e2e.log" \
    bash -c 'cd integrity-core-fr && npx playwright test' || true

  mkdir -p "$ARTIFACTS/playwright"
  cp -r integrity-core-fr/e2e-results "$ARTIFACTS/playwright/" 2>/dev/null || true
  cp -r integrity-core-fr/playwright-report "$ARTIFACTS/playwright/" 2>/dev/null || true
else
  record "smoke API" "SKIP" "backend no responde en :8080"
  record "E2E (Playwright)" "SKIP" "backend no responde en :8080"
  info "backend caído: se omite smoke y E2E (usa --up)"
fi

# ---------------------------------------------------------------------
# 5. Reporte
# ---------------------------------------------------------------------
REPORT="$ARTIFACTS/reporte.md"
{
  echo "# Reporte QA — $STAMP"
  echo
  echo "| Check | Estado | Detalle |"
  echo "|---|---|---|"
  for i in "${!NAMES[@]}"; do
    echo "| ${NAMES[$i]} | ${STATUSES[$i]} | ${DETAILS[$i]} |"
  done
  echo
  echo "Artefactos: \`qa-artifacts/$STAMP/\`"
} > "$REPORT"

# JSON para el triage automático
{
  echo "["
  first=1
  for i in "${!NAMES[@]}"; do
    ((first)) || echo ","
    first=0
    printf '{"name":"%s","status":"%s","detail":"%s"}' "${NAMES[$i]}" "${STATUSES[$i]}" "${DETAILS[$i]}"
  done
  echo
  echo "]"
} > "$ARTIFACTS/reporte.json"

FAILED=0
for s in "${STATUSES[@]}"; do [[ "$s" == "FAIL" ]] && FAILED=$((FAILED + 1)); done

echo
if ((FAILED == 0)); then
  ok "QA completa sin fallos — reporte: qa-artifacts/$STAMP/reporte.md"
else
  bad "$FAILED check(s) fallaron — reporte: qa-artifacts/$STAMP/reporte.md"
fi

if ((TRIAGE)); then
  info "abriendo issues de los fallos…"
  ./scripts/qa-triage.sh "$ARTIFACTS" || true
fi

((FAILED == 0)) && exit 0 || exit 1
