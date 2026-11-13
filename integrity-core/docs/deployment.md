# Deployment Guide

## Pasos de Deploy

1. **Pull images**: `docker compose -f docker-compose.production.yml pull`
2. **SSH Deploy**: Ejecutar `scripts/deploy-prod.sh --host <server>`
3. **Run Flyway**: `./mvnw flyway:migrate` en la carpeta `integrity-core/`
4. **Healthcheck**: `curl http://localhost:8081/health` + `curl http://localhost:8080/actuator/health`

## Variables de Entorno

| Variable | Valor | Descripción |
|----------|-------|-------------|
| `SPRING_PROFILES_ACTIVE` | `production` | Perfil Spring Boot |
| `REDIS_HOST` | `redis` | Host de Redis (Docker network) |
| `REDIS_PORT` | `6379` | Puerto de Redis |
| `JWT_SECRET` | (secret) | Clave de firma JWT (mín. 32 chars) |
| `ADMIN_REGISTRATION_CODE` | (secret) | Código para registrar administradores |
| `PROFESOR_REGISTRATION_CODE` | (secret) | Código para registrar profesores |
| `API_URL` | `/api/` | URL base para el frontend |

## Notas de Seguridad

- **HTTPS**: Certificado SSL gestionado por Certbot/Let's Encrypt. Nginx redirige HTTP→HTTPS (301).
- **JWT_SECRET**: Nunca en control de versión. Configurar en `.env` o variables del servidor.
- **CORS_ALLOWED_ORIGINS**: Solo permitir el dominio `https://cidem-integrity.example.com` en producción.
- **SSL Ciphers**: `HIGH:!aNULL:!MD5` — TLSv1.2 y TLSv1.3 únicamente.
