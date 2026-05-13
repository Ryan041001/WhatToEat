#!/usr/bin/env bash
set -euo pipefail

if [ ! -f .env ]; then
  echo "Missing .env. Copy .env.example to .env and fill production values first." >&2
  exit 1
fi

docker compose --env-file .env -f compose.prod.yaml up -d --build \
  --wait --wait-timeout "${DEPLOY_HEALTH_TIMEOUT_SECONDS:-120}"

echo "Services are healthy."

docker compose --env-file .env -f compose.prod.yaml ps
