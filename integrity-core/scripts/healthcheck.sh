#!/bin/bash
# =====================================================================
# Healthcheck script — verifies all services are responding
# Returns exit 0 only if ALL services pass, non-zero otherwise.
# =====================================================================
set -euo pipefail

FAILED=0

# Frontend (Angular SPA served by nginx)
echo "Checking frontend (http://localhost:8081/health)..."
if curl -sf --max-time 5 http://localhost:8081/health > /dev/null; then
  echo "  ✓ Frontend is UP"
else
  echo "  ✗ Frontend is DOWN"
  FAILED=$((FAILED + 1))
fi

# Backend (Spring Boot actuator)
echo "Checking backend (http://localhost:8080/actuator/health)..."
if curl -sf --max-time 5 http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
  echo "  ✓ Backend is UP"
else
  echo "  ✗ Backend is DOWN"
  FAILED=$((FAILED + 1))
fi

# Redis (TCP check)
echo "Checking Redis (localhost:6379)..."
if redis-cli -h localhost -p 6379 ping 2>/dev/null | grep -q 'PONG'; then
  echo "  ✓ Redis is UP"
else
  echo "  ✗ Redis is DOWN"
  FAILED=$((FAILED + 1))
fi

if [[ $FAILED -gt 0 ]]; then
  echo "⚠ $FAILED service(s) down"
  exit 1
fi

echo "✓ All services healthy"
exit 0
