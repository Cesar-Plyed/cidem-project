#!/usr/bin/env bash
# Arranca el backend en local (sin Docker) usando db/redis de docker-compose.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../integrity-core"
set -a; . ./.env; set +a
exec ./mvnw -q spring-boot:run
