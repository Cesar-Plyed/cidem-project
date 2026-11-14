#!/usr/bin/env bash
# =====================================================================
# qa-triage.sh — convierte los fallos de un reporte QA en issues de GitHub
#
# Uso: ./scripts/qa-triage.sh [qa-artifacts/<fecha>]
#      (sin argumento usa el reporte más reciente)
#
# Requiere gh autenticado (gh auth status) y permisos de issues en el repo.
# Es idempotente: si ya hay un issue abierto con el mismo título, no duplica.
# =====================================================================
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
ROOT="$(pwd)"

info() { printf '\033[1;34m[triage]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[ ok ]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*"; }

ARTIFACTS=""
DRY_RUN=0
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=1 ;;
    *) ARTIFACTS="$arg" ;;
  esac
done
if [[ -z "$ARTIFACTS" ]]; then
  ARTIFACTS="$(ls -1d qa-artifacts/*/ 2>/dev/null | sort | tail -1)"
fi
[[ -n "$ARTIFACTS" && -d "$ARTIFACTS" ]] || { echo "No hay reportes en qa-artifacts/" >&2; exit 2; }
ARTIFACTS="${ARTIFACTS%/}"

REPORT_JSON="$ARTIFACTS/reporte.json"
[[ -f "$REPORT_JSON" ]] || { echo "Falta $REPORT_JSON" >&2; exit 2; }

command -v gh >/dev/null 2>&1 || { echo "gh no está instalado" >&2; exit 2; }
gh auth status >/dev/null 2>&1 || { echo "gh no está autenticado (gh auth login)" >&2; exit 2; }

REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
SHA="$(git rev-parse --short HEAD 2>/dev/null || echo desconocido)"
BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo desconocida)"

# Etiqueta: usa "bug" si existe en el repo; si no, crea el issue sin etiqueta.
LABEL_ARGS=()
if gh label list --limit 100 --json name -q '.[].name' 2>/dev/null | grep -qx 'bug'; then
  LABEL_ARGS=(--label bug)
fi

TRIAGE_REPORT="$ARTIFACTS/triage.md"
{
  echo "# Triage — $(basename "$ARTIFACTS")"
  echo
  echo "Repo: \`$REPO\` · commit \`$SHA\` ($BRANCH)"
  echo
} > "$TRIAGE_REPORT"

CREATED=0; SKIPPED=0

while IFS=$'\t' read -r NAME DETAIL; do
  [[ -n "$NAME" ]] || continue
  TITLE="[QA] $NAME falla"

  EXISTING="$(gh issue list --state open --search "$TITLE in:title" --json number,title \
    -q ".[] | select(.title == \"$TITLE\") | .number" 2>/dev/null || true)"
  if [[ -n "$EXISTING" ]]; then
    ok "ya existe #$EXISTING — $TITLE"
    echo "- #$EXISTING (existente): $NAME" >> "$TRIAGE_REPORT"
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  LOG_NAME="$(sed -n 's/.*ver \(.*\)$/\1/p' <<<"$DETAIL")"
  BODY_FILE="$ARTIFACTS/.issue-body.md"
  {
    echo "Detectado por la suite automática de QA (\`scripts/qa.sh\`)."
    echo
    echo "| Campo | Valor |"
    echo "|---|---|"
    echo "| Check | \`$NAME\` |"
    echo "| Detalle | $DETAIL |"
    echo "| Commit | \`$SHA\` |"
    echo "| Rama | \`$BRANCH\` |"
    echo "| Fecha | $(date -Iseconds) |"
    echo
    if [[ -n "$LOG_NAME" && -f "$ARTIFACTS/logs/$LOG_NAME" ]]; then
      echo "<details><summary>Últimas líneas del log</summary>"
      echo
      echo '```'
      tail -n 40 "$ARTIFACTS/logs/$LOG_NAME"
      echo '```'
      echo
      echo "</details>"
      echo
    fi
    echo "Reproducir localmente:"
    echo
    echo '```bash'
    echo "./scripts/qa.sh"
    echo '```'
  } > "$BODY_FILE"

  if ((DRY_RUN)); then
    warn "[dry-run] crearía: $TITLE"
    echo "- (dry-run) $TITLE" >> "$TRIAGE_REPORT"
    SKIPPED=$((SKIPPED + 1))
    rm -f "$BODY_FILE"
    continue
  fi

  if gh issue create --repo "$REPO" --title "$TITLE" --body-file "$BODY_FILE" "${LABEL_ARGS[@]}" >/dev/null 2>&1; then
    ok "issue creado — $TITLE"
    echo "- $TITLE" >> "$TRIAGE_REPORT"
    CREATED=$((CREATED + 1))
  else
    warn "no se pudo crear el issue — $TITLE"
    echo "- (falló) $TITLE" >> "$TRIAGE_REPORT"
  fi
  rm -f "$BODY_FILE"
done < <(python3 - "$REPORT_JSON" <<'PY'
import json, sys
rows = json.load(open(sys.argv[1]))
for row in rows:
    if row.get("status") == "FAIL":
        print(f"{row['name']}\t{row.get('detail', '')}")
PY
)

echo
info "creados: $CREATED · ya existían: $SKIPPED · detalle en $TRIAGE_REPORT"
