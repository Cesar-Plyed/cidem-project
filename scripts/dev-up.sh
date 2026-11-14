#!/usr/bin/env bash
# =====================================================================
# dev-up.sh — levanta el stack completo de desarrollo con Docker
# Uso: ./scripts/dev-up.sh [--no-build]
# =====================================================================
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."
ROOT="$(pwd)"

info()  { printf '\033[1;34m[dev-up]\033[0m %s\n' "$*"; }
ok()    { printf '\033[1;32m[ ok ]\033[0m %s\n' "$*"; }
fail()  { printf '\033[1;31m[fail]\033[0m %s\n' "$*" >&2; exit 1; }

# ---------- 1. Requisitos -------------------------------------------------
command -v docker >/dev/null 2>&1 || fail "docker no está instalado."

if ! docker info >/dev/null 2>&1; then
  cat >&2 <<'EOF'
El demonio de Docker no está corriendo (o tu usuario no puede usarlo).

En Void Linux, con una cuenta con sudo:
    sudo ln -s /etc/sv/docker /var/service/     # habilita el servicio
    sudo sv start docker                        # arráncalo ahora

Luego vuelve a ejecutar este script. Si sigue fallando:
    sudo usermod -aG docker "$USER"   # y cierra/abre sesión
EOF
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  cat >&2 <<'EOF'
Falta el plugin/binario de Compose.

En Void Linux:
    sudo xbps-install -S docker-compose
EOF
  exit 1
fi

# ---------- 2. .env -------------------------------------------------------
if [[ ! -f "$ROOT/.env" ]]; then
  info "No hay .env; copiando .env.example"
  cp "$ROOT/.env.example" "$ROOT/.env"
fi
# shellcheck disable=SC1091
set -a; source "$ROOT/.env"; set +a

# ---------- 3. Recursos externos que espera docker-compose.yml -----------
docker network inspect integrity-net >/dev/null 2>&1 \
  || { info "creando red integrity-net"; docker network create integrity-net >/dev/null; }
docker volume inspect postgre-db >/dev/null 2>&1 \
  || { info "creando volumen postgre-db"; docker volume create postgre-db >/dev/null; }

# ---------- 4. Arriba -----------------------------------------------------
BUILD_FLAG=(--build)
[[ "${1:-}" == "--no-build" ]] && BUILD_FLAG=()

info "levantando db, redis, backend y frontend…"
"${COMPOSE[@]}" up -d "${BUILD_FLAG[@]}"

# ---------- 5. Esperar a que respondan -----------------------------------
wait_http() {
  local name="$1" url="$2" tries="${3:-90}"
  for ((i = 1; i <= tries; i++)); do
    if curl -fsS -o /dev/null --max-time 3 "$url" 2>/dev/null; then
      ok "$name responde ($url)"
      return 0
    fi
    sleep 2
  done
  fail "$name no respondió en $((tries * 2))s ($url). Revisa: ${COMPOSE[*]} logs --tail=80"
}

info "esperando al backend (Flyway + Hibernate validate)…"
wait_http "backend" "http://localhost:8080/actuator/health" 120

info "esperando al frontend…"
wait_http "frontend" "http://localhost:8081/" 60

cat <<EOF

$(ok "stack arriba")

  Frontend   http://localhost:8081
  Backend    http://localhost:8080
  Swagger    http://localhost:8080/swagger-ui/index.html
  Health     http://localhost:8080/actuator/health

  Logs       ${COMPOSE[*]} logs -f --tail=100
  Bajar      ${COMPOSE[*]} down
EOF
