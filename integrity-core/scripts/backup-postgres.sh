#!/bin/bash
# =====================================================================
# PostgreSQL Backup Script for CIDEM Integrity
# Usage: backup-postgres.sh (env vars or defaults)
# =====================================================================
set -euo pipefail

BACKUP_DIR="/opt/cidem-integrity/backups"
DB_HOST="${PG_HOST:-localhost}"
DB_PORT="${PG_PORT:-5432}"
DB_NAME="${PG_DB:-cidem_integrity}"
DB_USER="${PG_USER:-postgres}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/cidem-integrity_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" | gzip > "${BACKUP_FILE}"
echo "Backup saved to: ${BACKUP_FILE}"

# Clean backups older than 30 days
find "${BACKUP_DIR}" -name "cidem-integrity_*.sql.gz" -mtime +30 -delete
echo "Cleaned backups older than 30 days"
