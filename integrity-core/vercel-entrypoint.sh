#!/bin/sh
# Traduce las variables de Vercel a las de Spring Boot y arranca la app.
# Todo lo que ya venga definido explícitamente (SPRING_*) se respeta.
set -e

# --- Puerto -----------------------------------------------------------
# Vercel enruta al puerto de la variable PORT (configúrala en el proyecto: PORT=8080).
export SERVER_PORT="${PORT:-8080}"

# --- Postgres (Neon) --------------------------------------------------
# La app usa la conexión con pool; Flyway usa la directa (sin pool).
if [ -z "$SPRING_DATASOURCE_URL" ]; then
  if [ -z "$PGHOST" ] || [ -z "$PGDATABASE" ] || [ -z "$PGUSER" ] || [ -z "$PGPASSWORD" ]; then
    echo "ERROR: faltan PGHOST/PGDATABASE/PGUSER/PGPASSWORD. Conecta la integración de Neon al proyecto." >&2
    exit 1
  fi
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${PGHOST}/${PGDATABASE}?sslmode=require"
  export SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$PGUSER}"
  export SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$PGPASSWORD}"
  export SPRING_FLYWAY_URL="${SPRING_FLYWAY_URL:-jdbc:postgresql://${PGHOST_UNPOOLED:-$PGHOST}/${PGDATABASE}?sslmode=require}"
  export SPRING_FLYWAY_USER="${SPRING_FLYWAY_USER:-$PGUSER}"
  export SPRING_FLYWAY_PASSWORD="${SPRING_FLYWAY_PASSWORD:-$PGPASSWORD}"
fi

# Pool chico: cada instancia serverless abre sus propias conexiones.
export SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE="${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:-5}"

# --- Redis (Upstash) --------------------------------------------------
# Debe ser una URL TCP con TLS: rediss://default:<password>@<host>:6379
if [ -z "$SPRING_DATA_REDIS_URL" ]; then
  REDIS="${REDIS_URL:-$KV_URL}"
  if [ -n "$REDIS" ]; then
    export SPRING_DATA_REDIS_URL="$REDIS"
  else
    echo "AVISO: no hay REDIS_URL ni KV_URL; el caché de resultados fallará." >&2
  fi
fi

# --- Apagado limpio: Vercel manda SIGTERM y da 30 s de gracia ----------
export SERVER_SHUTDOWN="${SERVER_SHUTDOWN:-graceful}"
export SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE="${SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE:-20s}"

# JAVA_OPTS permite ajustar la JVM desde Vercel sin reconstruir la imagen.
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:--XX:+UseSerialGC -XX:TieredStopAtLevel=1} -XX:MaxRAMPercentage=75.0 -jar /app/app.jar