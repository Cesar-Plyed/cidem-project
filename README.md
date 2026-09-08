# Servidor SSH de despliegue — guía rápida

## 1. Antes de construir la imagen
Edita `ssh_keys/authorized_keys` y pega ahí las 4 llaves públicas del equipo
(una por línea). Si alguien no tiene una:
```bash
ssh-keygen -t ed25519 -C "nombre@equipo"
cat ~/.ssh/id_ed25519.pub   # copiar esta línea al archivo authorized_keys
```

## 2. Construir y levantar el servidor
```bash
cd infra
docker compose build
docker compose up -d
```
Se publica el puerto **2222 → 22** del contenedor (ajusta en
`docker-compose.yml` si prefieres exponerlo distinto, o usar
`network_mode: host` si el host ya vive en el tailnet y quieres que el
contenedor comparta directamente su IP de Tailscale).

Probar conexión:
```bash
ssh -p 2222 pawner@<IP-o-MagicDNS-de-Tailscale-del-host>
```

## 3. Preparar `/srv/app` dentro del contenedor (una sola vez)
```bash
ssh -p 2222 pawner@<host>
cd /srv/app
git clone <url-del-repo> .
# backend/ y frontend/ deben tener cada uno su propio podman-compose.yml
```

## 4. Llave de despliegue (deploy key) para GitHub Actions
Genera un par de llaves **exclusivo para CI** (no reutilices la personal):
```bash
ssh-keygen -t ed25519 -f deploy_key -C "github-actions-deploy" -N ""
```
- La **pública** (`deploy_key.pub`) se agrega también a
  `~/.ssh/authorized_keys` del usuario `pawner` dentro del servidor
  (además de las 4 personales).
- La **privada** (`deploy_key`) se guarda como secret de GitHub:
  `SSH_DEPLOY_KEY`.

## 5. Secrets a configurar en el repo de GitHub
`Settings → Secrets and variables → Actions`:

| Secret | Valor |
|---|---|
| `SSH_HOST` | IP de Tailscale (`100.x.x.x`) o MagicDNS (`servidor.tuTailnet.ts.net`) del host |
| `SSH_DEPLOY_KEY` | Contenido de la llave privada `deploy_key` generada en el paso 4 |
| `TS_OAUTH_CLIENT_ID` | Client ID de una OAuth client de Tailscale (admin console → Settings → OAuth clients) |
| `TS_OAUTH_SECRET` | Secret de esa misma OAuth client |

La OAuth client de Tailscale necesita el scope de **auth keys** y el tag
`tag:ci` debe existir en tu tailnet (ACLs) para que el runner efímero de
GitHub pueda unirse y alcanzar al servidor.

## 6. Flujo del redeploy automático
Cada `git push` a `main` dispara `.github/workflows/deploy.yml`:
1. El runner de GitHub se une al tailnet vía OAuth (efímero, se retira al terminar).
2. Se conecta por SSH como `pawner` usando la deploy key.
3. Dentro del servidor: `git reset --hard origin/main`, luego
   `podman-compose up -d --build` en `backend/` y `frontend/`.
4. Limpia imágenes viejas de Podman.

## Notas
- El contenedor corre `--privileged` porque Podman anidado (contenedores
  dentro de contenedor) lo requiere para namespaces/cgroups. Si el host
  no permite `--privileged` (por política de seguridad), la alternativa es
  instalar Podman directamente en el host y saltarte este Dockerfile.
- Recursos (4 CPU / 16 GB RAM) están limitados en `docker-compose.yml`
  vía `deploy.resources.limits` + `cpus`/`mem_limit` como respaldo, según
  qué motor de Compose uses.
- Tailscale **no** se instala dentro del contenedor porque ya está
  configurado en el host, tal como indicaste; el contenedor solo expone
  el puerto SSH para que el host lo sirva a través del tailnet.
