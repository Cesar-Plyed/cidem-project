# PostgreSQL Restore Guide

## Full Restore

```bash
gunzip -c /opt/cidem-integrity/backups/cidem-integrity_20260115_020000.sql.gz | psql -h localhost -U postgres -d cidem_integrity
```

## Point-in-Time Recovery

```bash
# 1. Restore full backup
gunzip -c /opt/cidem-integrity/backups/cidem-integrity_20260115_020000.sql.gz | psql -h localhost -U postgres -d cidem_integrity

# 2. Apply WAL logs up to target time
pg_restore --dbname=cidem_integrity --single-transaction /opt/cidem-integrity/wals/20260115_020000.wal
```

## Restore to Staging

```bash
# 1. Create new database
createdb cidem_staging

# 2. Restore backup
gunzip -c /opt/cidem-integrity/backups/cidem-integrity_20260115_020000.sql.gz | psql -h localhost -U postgres -d cidem_staging
```

## Variables de Entorno

El script `backup-postgres.sh` usa las siguientes variables (o valores por defecto):

| Variable | Default | Descripción |
|----------|---------|-------------|
| `PG_HOST` | `localhost` | Host de la base de datos |
| `PG_PORT` | `5432` | Puerto de PostgreSQL |
| `PG_DB` | `cidem_integrity` | Nombre de la base de datos |
| `PG_USER` | `postgres` | Usuario con privilegios de backup |

## Notas

- Los backups se guardan en `/opt/cidem-integrity/backups/` con formato `cidem-integrity_YYYYMMDD_HHMMSS.sql.gz`.
- Los archivos mayores a 30 días se eliminan automáticamente.
- Para restores en staging, crear la DB con `createdb` antes de aplicar el backup.
