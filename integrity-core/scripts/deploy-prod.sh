#!/bin/bash
# =====================================================================
# Production deploy script for CIDEM Integrity
# Usage: ./deploy-prod.sh --host <server> --user <user> [--dry-run]
# =====================================================================
set -euo pipefail

HOST="${DEPLOY_HOST:-prod-server.example.com}"
USER="${DEPLOY_USER:-ubuntu}"
REPO_PATH="${DEPLOY_PATH:-/opt/cidem-integrity}"
DRY_RUN=0
SSH_OPTS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)  HOST="$2";   shift 2 ;;
    --user)  USER="$2";   shift 2 ;;
    --path)  REPO_PATH="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    *)     echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

if [[ $DRY_RUN -eq 1 ]]; then
  SSH_OPTS="$SSH_OPTS --dry-run"
  echo "[DRY RUN] Would deploy to $USER@$HOST:$REPO_PATH"
fi

SSH_CMD="ssh -o StrictHostKeyChecking=no $USER@$HOST"

# Pull latest images
$SSH_CMD "docker compose -f $REPO_PATH/docker-compose.production.yml pull"

# Deploy (rolling restart)
$SSH_CMD "docker compose -f $REPO_PATH/docker-compose.production.yml up -d"

# Prune unused images
$SSH_CMD "docker image prune -f"

echo "Deploy to $HOST complete."
